-- Flyway Migration: V2__Sample_Data.sql
-- Sample data for data validation framework

-- Comparison config for Orders table
INSERT INTO comparison_config (table_name, enabled, description, created_by, last_modified_by)
VALUES ('Orders', 1, 'Daily validation of order amounts and counts', 'system', 'system');

-- Day-over-day config for Orders
INSERT INTO day_over_day_config (comparison_config_id, enabled, exclusion_condition)
VALUES (1, 1, 'status <> ''CANCELED''');

-- Column configs for Orders day-over-day comparison
INSERT INTO column_comparison_config (day_over_day_config_id, column_name, comparison_type,
                                      null_handling_strategy, blank_handling_strategy, na_handling_strategy)
VALUES (1, 'total_amount', 'PERCENTAGE', 'TREAT_AS_ZERO', 'TREAT_AS_ZERO', 'TREAT_AS_ZERO');

INSERT INTO column_comparison_config (day_over_day_config_id, column_name, comparison_type,
                                      null_handling_strategy, blank_handling_strategy, na_handling_strategy)
VALUES (1, 'order_count', 'ABSOLUTE', 'TREAT_AS_ZERO', 'TREAT_AS_ZERO', 'TREAT_AS_ZERO');

-- Thresholds for Orders columns
INSERT INTO threshold_config (column_comparison_config_id, threshold_value, severity, notification_enabled)
VALUES (1, 10.0000, 'HIGH', 1); -- 10% threshold for total_amount

INSERT INTO threshold_config (column_comparison_config_id, threshold_value, severity, notification_enabled)
VALUES (2, 50.0000, 'MEDIUM', 1); -- 50 units threshold for order_count

-- Comparison config for Products table
INSERT INTO comparison_config (table_name, enabled, description, created_by, last_modified_by)
VALUES ('Products', 1, 'Cross-table validation of product inventory', 'system', 'system');

-- Cross-table config for Products vs Inventory
INSERT INTO cross_table_config (source_comparison_config_id, target_table_name, join_condition, enabled)
VALUES (2, 'Inventory', 'Products.product_id = Inventory.product_id', 1);

-- Column configs for Products cross-table comparison
INSERT INTO column_comparison_config (cross_table_config_id, column_name, target_column_name, comparison_type,
                                      null_handling_strategy, blank_handling_strategy, na_handling_strategy)
VALUES (1, 'stock_qty', 'available_qty', 'EXACT', 'FAIL', 'FAIL', 'FAIL');

-- Thresholds for Products columns
INSERT INTO threshold_config (column_comparison_config_id, threshold_value, severity, notification_enabled)
VALUES (3, 0.0000, 'HIGH', 1); -- Exact match required for stock quantities

-- Email notification configs
INSERT INTO email_notification_config (email_address, severity_level, enabled)
VALUES ('alerts@example.com', 'HIGH', 1);

INSERT INTO email_notification_config (email_address, severity_level, enabled)
VALUES ('reports@example.com', 'MEDIUM', 1);

INSERT INTO email_notification_config (email_address, severity_level, enabled)
VALUES ('admin@example.com', 'LOW', 1);
