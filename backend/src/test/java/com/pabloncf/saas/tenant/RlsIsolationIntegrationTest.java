package com.pabloncf.saas.tenant;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class RlsIsolationIntegrationTest {

    static final UUID ORG_A = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    static final UUID ORG_B = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("saas_dashboard")
            .withUsername("saas_user")
            .withPassword("saas_pass");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        // App queries run as saas_app (non-superuser) — RLS is enforced
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", () -> "saas_app");
        registry.add("spring.datasource.password", () -> "saas_app_pass");
        // Flyway migrations run as saas_user (superuser) — can CREATE TABLE, CREATE ROLE, etc.
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
    }

    // JdbcTemplate backed by TenantAwareDataSource (saas_app) — subject to RLS
    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeAll
    static void insertTestOrganizations() {
        // Use the superuser connection to bypass RLS for test data setup
        HikariDataSource admin = new HikariDataSource();
        admin.setJdbcUrl(postgres.getJdbcUrl());
        admin.setUsername("saas_user");
        admin.setPassword("saas_pass");
        try {
            new JdbcTemplate(admin).update(
                    "INSERT INTO organizations (id, name, slug) VALUES (?, ?, ?), (?, ?, ?)",
                    ORG_A, "Org Alpha", "org-alpha",
                    ORG_B, "Org Beta",  "org-beta"
            );
        } finally {
            admin.close();
        }
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void tenantA_sees_only_its_own_organization() {
        TenantContext.setCurrentTenant(ORG_A);

        List<UUID> visible = queryVisibleOrgIds();

        assertThat(visible).containsExactly(ORG_A);
        assertThat(visible).doesNotContain(ORG_B);
    }

    @Test
    void tenantB_sees_only_its_own_organization() {
        TenantContext.setCurrentTenant(ORG_B);

        List<UUID> visible = queryVisibleOrgIds();

        assertThat(visible).containsExactly(ORG_B);
        assertThat(visible).doesNotContain(ORG_A);
    }

    @Test
    void no_tenant_in_context_returns_no_rows() {
        // TenantContext is cleared in @AfterEach — no tenant set here
        List<UUID> visible = queryVisibleOrgIds();

        assertThat(visible).isEmpty();
    }

    private List<UUID> queryVisibleOrgIds() {
        return jdbcTemplate.query(
                "SELECT id FROM organizations ORDER BY name",
                (rs, i) -> rs.getObject("id", UUID.class)
        );
    }
}
