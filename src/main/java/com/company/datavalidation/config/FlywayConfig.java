package com.company.datavalidation.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class FlywayConfig {

    /**
     * Custom Flyway migration strategy
     * This provides more control over the migration process
     * and logs detailed information about migrations
     */
    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            try {
                log.info("Starting database migration...");
                flyway.info();
                flyway.migrate();
                log.info("Database migration completed successfully");
            } catch (Exception e) {
                log.error("Database migration failed: {}", e.getMessage(), e);
                throw e;
            }
        };
    }

    /**
     * Configure Flyway programmatically
     * This allows for more advanced customization beyond what's in application.properties
     */
    @Bean
    public Flyway flyway() {
        return Flyway.configure()
                .dataSource(
                        "${spring.datasource.url}",
                        "${spring.datasource.username}",
                        "${spring.datasource.password}")
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .outOfOrder(true)
                .mixed(true)
                .load();
    }
}
