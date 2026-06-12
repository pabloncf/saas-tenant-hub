package com.pabloncf.saas.auth;

import com.pabloncf.saas.auth.dto.AuthResponse;
import com.pabloncf.saas.auth.dto.LoginRequest;
import com.pabloncf.saas.auth.dto.RefreshRequest;
import com.pabloncf.saas.auth.dto.RegisterRequest;
import com.pabloncf.saas.tenant.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    // Minimal authenticated endpoint — used to verify JWT validity and tenant resolution.
    // Will be replaced by a proper /users/me in Phase 4.
    @GetMapping("/me")
    public ResponseEntity<Map<String, String>> me(Authentication authentication) {
        return ResponseEntity.ok(Map.of(
                "userId",   authentication.getPrincipal().toString(),
                "tenantId", TenantContext.getCurrentTenant().toString(),
                "role",     authentication.getAuthorities().iterator().next().getAuthority()
        ));
    }
}
