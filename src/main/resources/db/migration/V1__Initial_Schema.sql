-- Flyway Migration: V1__Initial_Schema.sql
-- Initial database schema for data validation framework

-- Configuration tables
CREATE TABLE comparison_config (
                                   id INT IDENTITY(1,1) PRIMARY KEY,
                                   table_name NVARCHAR(100) NOT NULL,
                                   enabled BIT DEFAULT 1,
                                   description NVARCHAR(255),
                                   created_date DATETIME2 DEFAULT GETDATE(),
                                   last_modified_date DATETIME2 DEFAULT GETDATE(),
                                   created_by NVARCHAR(50),
                                   last_modified_by NVARCHAR(50)
);

CREATE TABLE day_over_day_config (
                                     id INT IDENTITY(1,1) PRIMARY KEY,
                                     comparison_config_id INT NOT NULL,
                                     enabled BIT DEFAULT 1,
                                     exclusion_condition NVARCHAR(500),
                                     FOREIGN KEY (comparison_config_id) REFERENCES comparison_config(id)
);

CREATE TABLE cross_table_config (
                                    id INT IDENTITY(1,1) PRIMARY KEY,
                                    source_comparison_config_id INT NOT NULL,
                                    target_table_name NVARCHAR(100) NOT NULL,
                                    join_condition NVARCHAR(500) NOT NULL,
                                    enabled BIT DEFAULT 1,
                                    FOREIGN KEY (source_comparison_config_id) REFERENCES comparison_config(id)
);

CREATE TABLE column_comparison_config (
                                          id INT IDENTITY(1,1) PRIMARY KEY,
                                          day_over_day_config_id INT NULL,
                                          cross_table_config_id INT NULL,
                                          column_name NVARCHAR(100) NOT NULL,
                                          target_column_name NVARCHAR(100) NULL,
                                          comparison_type NVARCHAR(20) NOT NULL, -- PERCENTAGE, ABSOLUTE, EXACT
                                          null_handling_strategy NVARCHAR(20) NOT NULL, -- IGNORE, TREAT_AS_ZERO, TREAT_AS_NULL, FAIL
                                          blank_handling_strategy NVARCHAR(20) NOT NULL, -- IGNORE, TREAT_AS_ZERO, TREAT_AS_NULL, FAIL
                                          na_handling_strategy NVARCHAR(20) NOT NULL, -- IGNORE, TREAT_AS_ZERO, TREAT_AS_NULL, FAIL
                                          FOREIGN KEY (day_over_day_config_id) REFERENCES day_over_day_config(id),
                                          FOREIGN KEY (cross_table_config_id) REFERENCES cross_table_config(id),
                                          CHECK (day_over_day_config_id IS NOT NULL OR cross_table_config_id IS NOT NULL)
);

CREATE TABLE threshold_config (
                                  id INT IDENTITY(1,1) PRIMARY KEY,
                                  column_comparison_config_id INT NOT NULL,
                                  threshold_value DECIMAL(18,4) NOT NULL,
                                  severity NVARCHAR(10) NOT NULL, -- HIGH, MEDIUM, LOW
                                  notification_enabled BIT DEFAULT 1,
                                  FOREIGN KEY (column_comparison_config_id) REFERENCES column_comparison_config(id)
);

CREATE TABLE email_notification_config (
                                           id INT IDENTITY(1,1) PRIMARY KEY,
                                           email_address NVARCHAR(100) NOT NULL,
                                           severity_level NVARCHAR(10) NOT NULL, -- HIGH, MEDIUM, LOW
                                           enabled BIT DEFAULT 1
);

-- Validation result tables
CREATE TABLE validation_result (
                                   id INT IDENTITY(1,1) PRIMARY KEY,
                                   comparison_config_id INT NOT NULL,
                                   execution_date DATETIME2 DEFAULT GETDATE(),
                                   success BIT NOT NULL,
                                   error_message NVARCHAR(MAX),
                                   execution_time_ms INT,
                                   FOREIGN KEY (comparison_config_id) REFERENCES comparison_config(id)
);

CREATE TABLE validation_detail_result (
                                          id INT IDENTITY(1,1) PRIMARY KEY,
                                          validation_result_id INT NOT NULL,
                                          column_comparison_config_id INT NOT NULL,
                                          threshold_exceeded BIT NOT NULL,
                                          actual_value DECIMAL(18,4),
                                          expected_value DECIMAL(18,4),
                                          difference_value DECIMAL(18,4),
                                          difference_percentage DECIMAL(18,4),
                                          FOREIGN KEY (validation_result_id) REFERENCES validation_result(id),
                                          FOREIGN KEY (column_comparison_config_id) REFERENCES column_comparison_config(id)
);

-- Indexes for performance
CREATE INDEX IX_comparison_config_table_name ON comparison_config(table_name);
CREATE INDEX IX_comparison_config_enabled ON comparison_config(enabled);
CREATE INDEX IX_day_over_day_config_comparison_config_id ON day_over_day_config(comparison_config_id);
CREATE INDEX IX_day_over_day_config_enabled ON day_over_day_config(enabled);
CREATE INDEX IX_cross_table_config_source_comparison_config_id ON cross_table_config(source_comparison_config_id);
CREATE INDEX IX_cross_table_config_enabled ON cross_table_config(enabled);
CREATE INDEX IX_column_comparison_config_day_over_day_config_id ON column_comparison_config(day_over_day_config_id);
CREATE INDEX IX_column_comparison_config_cross_table_config_id ON column_comparison_config(cross_table_config_id);
CREATE INDEX IX_threshold_config_column_comparison_config_id ON threshold_config(column_comparison_config_id);
CREATE INDEX IX_email_notification_config_severity_level ON email_notification_config(severity_level);
CREATE INDEX IX_validation_result_comparison_config_id ON validation_result(comparison_config_id);
CREATE INDEX IX_validation_result_execution_date ON validation_result(execution_date);
CREATE INDEX IX_validation_result_success ON validation_result(success);
CREATE INDEX IX_validation_detail_result_validation_result_id ON validation_detail_result(validation_result_id);
CREATE INDEX IX_validation_detail_result_threshold_exceeded ON validation_detail_result(threshold_exceeded);
