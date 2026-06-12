package com.pabloncf.saas.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@EnableConfigurationProperties(FlywayProperties.class)
public class FlywayConfig {

    // Flyway auto-config calls setUrl() on the primary DataSource, which fails
    // because TenantAwareDataSource is a custom wrapper without that method.
    // We define Flyway explicitly using adminDataSource (saas_user, superuser)
    // so it can run DDL migrations and bypass RLS policies.
    @Bean
    public Flyway flyway(@Qualifier("adminDataSource") DataSource adminDataSource,
                         FlywayProperties props) {
        return Flyway.configure()
                .dataSource(adminDataSource)
                .locations(props.getLocations().toArray(String[]::new))
                .cleanOnValidationError(props.isCleanOnValidationError())
                .load();
    }

    @Bean
    public FlywayMigrationInitializer flywayInitializer(Flyway flyway) {
        return new FlywayMigrationInitializer(flyway, null);
    }
}
