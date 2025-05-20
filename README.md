# Data Validation Framework

A flexible, reusable framework using Spring Boot, Java, and Gradle for comparing, validating, and threshold-monitoring database tables.

## Overview

This framework enables data validation across database tables through:

1. Day-over-day comparisons within tables
2. Cross-table comparisons between related tables
3. Validation against configurable thresholds
4. Detailed reporting and alerting

## Key Features

- **Flexible Configuration**: Database-driven settings for tables, columns, comparison types, and thresholds
- **Multiple Comparison Types**: Supports absolute difference, percentage change, and exact match comparisons
- **Special Value Handling**: Configurable strategies for NULL, blank, and N/A values
- **Comprehensive Reporting**: Generates detailed validation reports with export capabilities
- **Alerting System**: Email notifications for validation failures based on severity
- **REST API**: Complete API for configuration management and validation execution
- **Scheduled Execution**: Automatic daily validation with configurable schedules

## Technical Details

- **Language**: Java 17
- **Framework**: Spring Boot 3.x
- **Database**: Microsoft SQL Server
- **Build System**: Gradle 8.x

## Architecture

The framework follows a modular design with the following components:

1. **Configuration Module**: Database entities for storing validation settings
2. **Core Comparison Engine**: Implements various comparison strategies
3. **Data Access Layer**: Handles dynamic database access
4. **Validation Service**: Applies comparison logic and threshold validation
5. **Reporting Service**: Generates reports and notifications
6. **Execution Module**: Manages validation execution workflow
7. **API Layer**: REST endpoints for management and integration

## Getting Started

### Prerequisites

- Java 17+
- Gradle 8+
- Microsoft SQL Server

### Database Setup

1. Create a new database for the validation framework
2. Run the database schema script at `src/main/resources/db/schema.sql`

### Configuration

The application properties can be customized in `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=datavalidation;encrypt=true;trustServerCertificate=true;
spring.datasource.username=sa
spring.datasource.password=YourStrongPassword
```

### Building and Running

```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun
```

The application will be available at `http://localhost:8080`

## API Documentation

Once the application is running, the API documentation is available at:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`

### Key API Endpoints

- **Configuration Management**: `/api/v1/configs/**`
- **Validation Execution**: `/api/v1/executions/**`
- **Alerts**: `/api/v1/alerts/**`
- **Reports**: `/api/v1/reports/**`

## Extending the Framework

The framework is designed for extensibility:

1. **New Comparison Types**: Extend `AbstractComparator` and implement your logic
2. **Custom Validations**: Add new validation rules by extending existing services
3. **Additional Reporting Formats**: Implement new exporters in `ReportGenerator`

## Sample Usage Scenarios

### Day-over-Day Sales Validation

```sql
INSERT INTO comparison_config (table_name, enabled, description)
VALUES ('daily_sales', 1, 'Daily sales validation');

INSERT INTO day_over_day_config (comparison_config_id, enabled)
VALUES (1, 1);

INSERT INTO column_comparison_config (day_over_day_config_id, column_name, comparison_type, 
    null_handling_strategy, blank_handling_strategy, na_handling_strategy)
VALUES (1, 'total_revenue', 'PERCENTAGE', 'TREAT_AS_ZERO', 'TREAT_AS_ZERO', 'TREAT_AS_ZERO');

INSERT INTO threshold_config (column_comparison_config_id, threshold_value, severity, notification_enabled)
VALUES (1, 15.0000, 'HIGH', 1); -- 15% threshold
```

### Cross-Table Inventory Reconciliation

```sql
INSERT INTO comparison_config (table_name, enabled, description)
VALUES ('warehouse_inventory', 1, 'Inventory reconciliation');

INSERT INTO cross_table_config (source_comparison_config_id, target_table_name, join_condition, enabled)
VALUES (1, 'system_inventory', 'warehouse_inventory.sku = system_inventory.sku', 1);

INSERT INTO column_comparison_config (cross_table_config_id, column_name, target_column_name, comparison_type, 
    null_handling_strategy, blank_handling_strategy, na_handling_strategy)
VALUES (1, 'quantity', 'available_quantity', 'ABSOLUTE', 'FAIL', 'FAIL', 'FAIL');

INSERT INTO threshold_config (column_comparison_config_id, threshold_value, severity, notification_enabled)
VALUES (1, 5.0000, 'HIGH', 1); -- 5 unit threshold
```

## License

This project is licensed under the MIT License.
