package com.pabloncf.saas.config;

import com.pabloncf.saas.tenant.TenantAwareDataSource;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

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
        hikari.setJdbcUrl(url);
        hikari.setUsername(username);
        hikari.setPassword(password);
        // Disable fail-fast so HikariCP does not try to connect during startup.
        // The saas_app role is created by Flyway (V2) which runs before any JPA query.
        hikari.setInitializationFailTimeout(-1L);

        return new TenantAwareDataSource(hikari);
    }
}
