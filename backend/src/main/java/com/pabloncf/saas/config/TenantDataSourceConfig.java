package com.pabloncf.saas.config;

import com.pabloncf.saas.tenant.TenantAwareDataSource;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class TenantDataSourceConfig {

    @Bean
    @Primary
    public DataSource dataSource(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password) {

        HikariDataSource hikari = new HikariDataSource();
        hikari.setPoolName("app-pool");
        hikari.setJdbcUrl(url);
        hikari.setUsername(username);
        hikari.setPassword(password);
        // Disable fail-fast: saas_app is created by Flyway (V2) before the first actual JPA query.
        hikari.setInitializationFailTimeout(-1L);

        return new TenantAwareDataSource(hikari);
    }

    // Admin datasource connects as saas_user (superuser) — bypasses RLS.
    // Used exclusively by AuthService for privileged cross-tenant operations
    // (login, register, refresh) that must read/write across tenant boundaries.
    @Bean
    @Qualifier("adminDataSource")
    public DataSource adminDataSource(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.flyway.user}") String username,
            @Value("${spring.flyway.password}") String password) {

        HikariDataSource hikari = new HikariDataSource();
        hikari.setPoolName("admin-pool");
        hikari.setJdbcUrl(url);
        hikari.setUsername(username);
        hikari.setPassword(password);
        hikari.setMaximumPoolSize(5);

        return hikari;
    }

    // Primary JPA transaction manager — used by all @Transactional without an explicit qualifier.
    // Must be declared explicitly because adminTransactionManager below causes Spring Boot's
    // JpaTransactionManagerAutoConfiguration to back off (@ConditionalOnMissingBean), leaving
    // no bean named 'transactionManager' and breaking every service-layer transaction.
    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }

    @Bean
    @Qualifier("adminTransactionManager")
    public PlatformTransactionManager adminTransactionManager(
            @Qualifier("adminDataSource") DataSource adminDataSource) {
        return new DataSourceTransactionManager(adminDataSource);
    }
}
