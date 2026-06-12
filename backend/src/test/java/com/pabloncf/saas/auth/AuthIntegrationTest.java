package com.pabloncf.saas.auth;

import com.pabloncf.saas.auth.dto.AuthResponse;
import com.pabloncf.saas.auth.dto.LoginRequest;
import com.pabloncf.saas.auth.dto.RefreshRequest;
import com.pabloncf.saas.auth.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AuthIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("saas_dashboard")
            .withUsername("saas_user")
            .withPassword("saas_pass");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", () -> "saas_app");
        registry.add("spring.datasource.password", () -> "saas_app_pass");
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
    }

    @Autowired
    TestRestTemplate rest;

    @Test
    void register_returns_201_with_tokens() {
        var req = new RegisterRequest("Alice", "alice@example.com", "password123", "Alice Corp");

        var res = rest.postForEntity("/api/v1/auth/register", req, AuthResponse.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody().accessToken()).isNotBlank();
        assertThat(res.getBody().refreshToken()).isNotBlank();
    }

    @Test
    void duplicate_email_returns_409() {
        var req = new RegisterRequest("Bob", "bob@example.com", "password123", "Bob Corp");
        rest.postForEntity("/api/v1/auth/register", req, String.class);

        var duplicate = rest.postForEntity("/api/v1/auth/register", req, String.class);

        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void login_with_valid_credentials_returns_tokens() {
        var email = "carol@example.com";
        rest.postForEntity("/api/v1/auth/register",
                new RegisterRequest("Carol", email, "password123", "Carol Corp"), AuthResponse.class);

        var res = rest.postForEntity("/api/v1/auth/login",
                new LoginRequest(email, "password123"), AuthResponse.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().accessToken()).isNotBlank();
    }

    @Test
    void login_with_wrong_password_returns_401() {
        var email = "dave@example.com";
        rest.postForEntity("/api/v1/auth/register",
                new RegisterRequest("Dave", email, "password123", "Dave Corp"), AuthResponse.class);

        var res = rest.postForEntity("/api/v1/auth/login",
                new LoginRequest(email, "wrongpassword"), String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protected_endpoint_without_token_returns_401() {
        var res = rest.getForEntity("/api/v1/auth/me", String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protected_endpoint_with_valid_token_returns_200() {
        var authRes = rest.postForEntity("/api/v1/auth/register",
                new RegisterRequest("Eve", "eve@example.com", "password123", "Eve Corp"),
                AuthResponse.class);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authRes.getBody().accessToken());

        var res = rest.exchange("/api/v1/auth/me", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).contains("userId", "tenantId", "role");
    }

    @Test
    void refresh_rotates_token_and_old_token_is_invalid() {
        var authRes = rest.postForEntity("/api/v1/auth/register",
                new RegisterRequest("Frank", "frank@example.com", "password123", "Frank Corp"),
                AuthResponse.class);
        String originalRefreshToken = authRes.getBody().refreshToken();

        var refreshRes = rest.postForEntity("/api/v1/auth/refresh",
                new RefreshRequest(originalRefreshToken), AuthResponse.class);

        assertThat(refreshRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshRes.getBody().refreshToken()).isNotEqualTo(originalRefreshToken);

        // Old token must be rejected after rotation
        var reuseRes = rest.postForEntity("/api/v1/auth/refresh",
                new RefreshRequest(originalRefreshToken), String.class);
        assertThat(reuseRes.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
