package com.pabloncf.saas.auth;

import com.pabloncf.saas.auth.domain.Role;
import com.pabloncf.saas.auth.dto.AuthResponse;
import com.pabloncf.saas.auth.dto.LoginRequest;
import com.pabloncf.saas.auth.dto.RefreshRequest;
import com.pabloncf.saas.auth.dto.RegisterRequest;
import com.pabloncf.saas.common.security.JwtService;
import com.pabloncf.saas.config.JwtConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private final JdbcTemplate adminJdbc;
    private final TransactionTemplate adminTx;
    private final JwtService jwtService;
    private final JwtConfig jwtConfig;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            @Qualifier("adminDataSource") DataSource adminDataSource,
            @Qualifier("adminTransactionManager") PlatformTransactionManager adminTxManager,
            JwtService jwtService,
            JwtConfig jwtConfig,
            PasswordEncoder passwordEncoder) {
        this.adminJdbc = new JdbcTemplate(adminDataSource);
        this.adminTx   = new TransactionTemplate(adminTxManager);
        this.jwtService = jwtService;
        this.jwtConfig  = jwtConfig;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse register(RegisterRequest request) {
        return adminTx.execute(status -> {
            Boolean emailExists = adminJdbc.queryForObject(
                    "SELECT EXISTS(SELECT 1 FROM users WHERE email = ?)",
                    Boolean.class, request.email()
            );
            if (Boolean.TRUE.equals(emailExists)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
            }

            UUID orgId = UUID.randomUUID();
            String slug = generateUniqueSlug(request.organizationName());
            adminJdbc.update(
                    "INSERT INTO organizations (id, name, slug) VALUES (?, ?, ?)",
                    orgId, request.organizationName(), slug
            );

            UUID userId = UUID.randomUUID();
            adminJdbc.update(
                    "INSERT INTO users (id, email, password_hash, full_name) VALUES (?, ?, ?, ?)",
                    userId,
                    request.email(),
                    passwordEncoder.encode(request.password()),
                    request.fullName()
            );

            adminJdbc.update(
                    "INSERT INTO organization_members (id, organization_id, user_id, role) VALUES (?, ?, ?, ?)",
                    UUID.randomUUID(), orgId, userId, Role.OWNER.name()
            );

            return buildAuthResponse(userId, orgId, Role.OWNER);
        });
    }

    public AuthResponse login(LoginRequest request) {
        record UserRow(UUID id, String passwordHash) {}

        List<UserRow> results = adminJdbc.query(
                "SELECT id, password_hash FROM users WHERE email = ?",
                (rs, i) -> new UserRow(rs.getObject("id", UUID.class), rs.getString("password_hash")),
                request.email()
        );

        if (results.isEmpty() || !passwordEncoder.matches(request.password(), results.get(0).passwordHash())) {
            // Same exception for both cases — prevents user enumeration
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        UUID userId = results.get(0).id();

        record MemberRow(UUID orgId, String role) {}
        List<MemberRow> memberships = adminJdbc.query(
                "SELECT organization_id, role FROM organization_members WHERE user_id = ? ORDER BY created_at ASC LIMIT 1",
                (rs, i) -> new MemberRow(rs.getObject("organization_id", UUID.class), rs.getString("role")),
                userId
        );

        if (memberships.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No organization found for user");
        }

        UUID orgId = memberships.get(0).orgId();
        Role role  = Role.valueOf(memberships.get(0).role());

        return buildAuthResponse(userId, orgId, role);
    }

    public AuthResponse refresh(RefreshRequest request) {
        String hash = hashToken(request.refreshToken());

        record TokenRow(UUID id, UUID userId, UUID orgId, OffsetDateTime expiresAt, boolean revoked) {}

        List<TokenRow> rows = adminJdbc.query(
                "SELECT id, user_id, organization_id, expires_at, revoked FROM refresh_tokens WHERE token_hash = ?",
                (rs, i) -> new TokenRow(
                        rs.getObject("id",              UUID.class),
                        rs.getObject("user_id",         UUID.class),
                        rs.getObject("organization_id", UUID.class),
                        rs.getObject("expires_at",      OffsetDateTime.class),
                        rs.getBoolean("revoked")
                ),
                hash
        );

        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        TokenRow token = rows.get(0);

        if (token.revoked()) {
            // Reuse attack detected — revoke all active tokens for this user+org
            adminJdbc.update(
                    "UPDATE refresh_tokens SET revoked = TRUE WHERE user_id = ? AND organization_id = ?",
                    token.userId(), token.orgId()
            );
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token already used");
        }

        if (token.expiresAt().isBefore(OffsetDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }

        adminJdbc.update("UPDATE refresh_tokens SET revoked = TRUE WHERE id = ?", token.id());

        String roleStr = adminJdbc.queryForObject(
                "SELECT role FROM organization_members WHERE user_id = ? AND organization_id = ?",
                String.class, token.userId(), token.orgId()
        );

        return buildAuthResponse(token.userId(), token.orgId(), Role.valueOf(roleStr));
    }

    private AuthResponse buildAuthResponse(UUID userId, UUID orgId, Role role) {
        String accessToken   = jwtService.generateAccessToken(userId, orgId, role);
        String refreshToken  = storeRefreshToken(userId, orgId);
        return new AuthResponse(accessToken, refreshToken);
    }

    private String storeRefreshToken(UUID userId, UUID orgId) {
        String value    = UUID.randomUUID().toString();
        String hash     = hashToken(value);
        OffsetDateTime expiresAt = OffsetDateTime.now()
                .plus(jwtConfig.getRefreshExpiration(), ChronoUnit.MILLIS);

        adminJdbc.update(
                "INSERT INTO refresh_tokens (id, token_hash, user_id, organization_id, expires_at) VALUES (?, ?, ?, ?, ?)",
                UUID.randomUUID(), hash, userId, orgId, expiresAt
        );
        return value;
    }

    private String hashToken(String token) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte b : bytes) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private String generateUniqueSlug(String name) {
        String base = name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-");

        if (isSlugAvailable(base)) return base;

        for (int i = 2; i <= 99; i++) {
            String candidate = base + "-" + i;
            if (isSlugAvailable(candidate)) return candidate;
        }
        // Fallback: append random suffix
        return base + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private boolean isSlugAvailable(String slug) {
        Boolean exists = adminJdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM organizations WHERE slug = ?)", Boolean.class, slug
        );
        return !Boolean.TRUE.equals(exists);
    }
}
