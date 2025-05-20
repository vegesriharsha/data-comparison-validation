package com.company.datavalidation.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Repository
@RequiredArgsConstructor
@Slf4j
public class DynamicTableRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Get data from a table for the current day
     * @param tableName Name of the table
     * @param columnNames Columns to select
     * @param dateColumn Date column to filter on
     * @param exclusionCondition Optional exclusion condition
     * @return List of maps where each map represents a row with column name as key
     */
    public List<Map<String, Object>> getDataForCurrentDay(String tableName, List<String> columnNames,
                                                          String dateColumn, String exclusionCondition) {
        LocalDate today = LocalDate.now();
        return getDataForDate(tableName, columnNames, dateColumn, today, exclusionCondition);
    }

    /**
     * Get data from a table for a specific date
     * @param tableName Name of the table
     * @param columnNames Columns to select
     * @param dateColumn Date column to filter on
     * @param date Date to filter on
     * @param exclusionCondition Optional exclusion condition
     * @return List of maps where each map represents a row with column name as key
     */
    public List<Map<String, Object>> getDataForDate(String tableName, List<String> columnNames,
                                                    String dateColumn, LocalDate date, String exclusionCondition) {
        // Format columns for select clause
        String columnsClause = columnNames.stream().collect(Collectors.joining(", "));

        // Build query with modern text block
        String query = """
            SELECT %s
            FROM %s
            WHERE CONVERT(date, %s) = ?
            %s
            """.formatted(
                columnsClause,
                tableName,
                dateColumn,
                exclusionCondition != null && !exclusionCondition.isEmpty()
                        ? "AND " + exclusionCondition
                        : ""
        );

        log.debug("Executing query for date {}: {}", date, query);
        return jdbcTemplate.query(query, this::mapRowWithColumns, date);
    }

    /**
     * Get data from a table for the previous day
     * @param tableName Name of the table
     * @param columnNames Columns to select
     * @param dateColumn Date column to filter on
     * @param exclusionCondition Optional exclusion condition
     * @return List of maps where each map represents a row with column name as key
     */
    public List<Map<String, Object>> getDataForPreviousDay(String tableName, List<String> columnNames,
                                                           String dateColumn, String exclusionCondition) {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        return getDataForDate(tableName, columnNames, dateColumn, yesterday, exclusionCondition);
    }

    /**
     * Execute a cross-table comparison query
     * @param sourceTable Source table name
     * @param targetTable Target table name
     * @param sourceColumns Source columns to select
     * @param targetColumns Target columns to select
     * @param joinCondition Join condition between tables
     * @param dateColumn Date column to filter on
     * @param exclusionCondition Optional exclusion condition
     * @return List of maps where each map represents a row with column name as key
     */
    public List<Map<String, Object>> executeCrossTableQuery(String sourceTable, String targetTable,
                                                            List<String> sourceColumns, List<String> targetColumns,
                                                            String joinCondition, String dateColumn,
                                                            String exclusionCondition) {
        // Format source columns for select clause
        String sourceColumnsClause = sourceColumns.stream()
                .map(col -> "s." + col + " AS s_" + col)
                .collect(Collectors.joining(", "));

        // Format target columns for select clause
        String targetColumnsClause = targetColumns.stream()
                .map(col -> "t." + col + " AS t_" + col)
                .collect(Collectors.joining(", "));

        // Build the combined columns clause
        String columnsClause = String.join(", ", sourceColumnsClause, targetColumnsClause);

        // Build query with modern text block
        String query = """
            SELECT %s
            FROM %s s
            JOIN %s t ON %s
            WHERE CONVERT(date, s.%s) = CAST(GETDATE() AS date)
            %s
            """.formatted(
                columnsClause,
                sourceTable,
                targetTable,
                joinCondition,
                dateColumn,
                exclusionCondition != null && !exclusionCondition.isEmpty()
                        ? "AND " + exclusionCondition
                        : ""
        );

        log.debug("Executing cross-table query: {}", query);
        return jdbcTemplate.query(query, this::mapRow);
    }

    /**
     * Execute an aggregate query on a table
     * @param tableName Name of the table
     * @param aggregateFunction Aggregate function (SUM, AVG, COUNT, MIN, MAX)
     * @param columnName Column to aggregate
     * @param dateColumn Date column to filter on
     * @param date Date to filter on
     * @param exclusionCondition Optional exclusion condition
     * @return Aggregate value
     */
    public BigDecimal executeAggregateQuery(String tableName, String aggregateFunction, String columnName,
                                            String dateColumn, LocalDate date, String exclusionCondition) {
        // Build query with modern text block
        String query = """
            SELECT %s(%s) AS result
            FROM %s
            WHERE CONVERT(date, %s) = ?
            %s
            """.formatted(
                aggregateFunction,
                columnName,
                tableName,
                dateColumn,
                exclusionCondition != null && !exclusionCondition.isEmpty()
                        ? "AND " + exclusionCondition
                        : ""
        );

        log.debug("Executing aggregate query for date {}: {}", date, query);
        return jdbcTemplate.queryForObject(query, BigDecimal.class, date);
    }

    /**
     * Row mapper for query results using Java streams
     */
    private Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        // Use IntStream to iterate through column indices (1-based)
        return IntStream.rangeClosed(1, columnCount)
                .boxed()
                .collect(Collectors.toMap(
                        i -> {
                            try {
                                return metaData.getColumnName(i);
                            } catch (SQLException e) {
                                log.error("Error getting column name for index {}", i, e);
                                return "column_" + i; // Fallback column name
                            }
                        },
                        i -> {
                            try {
                                return rs.getObject(i);
                            } catch (SQLException e) {
                                log.error("Error getting column value for index {}", i, e);
                                return null;
                            }
                        },
                        // In case of duplicate keys (should not happen in SQL results)
                        (existing, replacement) -> {
                            log.warn("Duplicate column name found in result set, using first value");
                            return existing;
                        }
                ));
    }

    /**
     * Row mapper optimized for specified columns using Java streams
     */
    private Map<String, Object> mapRowWithColumns(ResultSet rs, int rowNum) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        // Use IntStream to iterate through column indices (1-based)
        return IntStream.rangeClosed(1, columnCount)
                .boxed()
                .collect(Collectors.toMap(
                        i -> {
                            try {
                                return metaData.getColumnName(i);
                            } catch (SQLException e) {
                                log.error("Error getting column name for index {}", i, e);
                                return "column_" + i; // Fallback column name
                            }
                        },
                        i -> {
                            try {
                                return rs.getObject(i);
                            } catch (SQLException e) {
                                log.error("Error getting column value for index {}", i, e);
                                return null;
                            }
                        }
                ));
    }
}
