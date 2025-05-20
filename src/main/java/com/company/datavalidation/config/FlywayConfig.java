package com.company.datavalidation.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class FlywayConfig {

    private static final Logger logger = LoggerFactory.getLogger(FlywayConfig.class);

    /**
     * Custom Flyway migration strategy
     * This provides more control over the migration process
     * and logs detailed information about migrations
     */
    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            // Clean the database (only in development)
            // flyway.clean(); // Uncomment for development/testing only!

            // Get the current migration info
            try {
                logger.info("Starting database migration...");
                flyway.info();

                // Run the migration
                flyway.migrate();

                // Log migration success
                logger.info("Database migration completed successfully");
            } catch (Exception e) {
                logger.error("Database migration failed: " + e.getMessage(), e);
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
