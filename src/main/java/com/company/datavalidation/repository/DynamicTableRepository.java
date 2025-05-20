package com.company.datavalidation.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class DynamicTableRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public DynamicTableRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Execute a query and return the results as a list of maps
     * @param query SQL query to execute
     * @return List of maps where each map represents a row with column name as key
     */
    public List<Map<String, Object>> executeQuery(String query) {
        return jdbcTemplate.query(query, new RowMapper<Map<String, Object>>() {
            @Override
            public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                    String columnName = rs.getMetaData().getColumnName(i);
                    Object value = rs.getObject(i);
                    row.put(columnName, value);
                }
                return row;
            }
        });
    }

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
        StringBuilder queryBuilder = new StringBuilder("SELECT ");

        // Add columns to select clause
        for (int i = 0; i < columnNames.size(); i++) {
            queryBuilder.append(columnNames.get(i));
            if (i < columnNames.size() - 1) {
                queryBuilder.append(", ");
            }
        }

        queryBuilder.append(" FROM ").append(tableName);
        queryBuilder.append(" WHERE CONVERT(date, ").append(dateColumn).append(") = ?");

        if (exclusionCondition != null && !exclusionCondition.trim().isEmpty()) {
            queryBuilder.append(" AND ").append(exclusionCondition);
        }

        return jdbcTemplate.query(queryBuilder.toString(), new RowMapper<Map<String, Object>>() {
            @Override
            public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
                Map<String, Object> row = new HashMap<>();
                for (String columnName : columnNames) {
                    Object value = rs.getObject(columnName);
                    row.put(columnName, value);
                }
                return row;
            }
        }, date);
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
        StringBuilder queryBuilder = new StringBuilder("SELECT ");

        // Add source columns
        for (int i = 0; i < sourceColumns.size(); i++) {
            queryBuilder.append("s.").append(sourceColumns.get(i)).append(" AS s_").append(sourceColumns.get(i));
            queryBuilder.append(", ");
        }

        // Add target columns
        for (int i = 0; i < targetColumns.size(); i++) {
            queryBuilder.append("t.").append(targetColumns.get(i)).append(" AS t_").append(targetColumns.get(i));
            if (i < targetColumns.size() - 1) {
                queryBuilder.append(", ");
            }
        }

        queryBuilder.append(" FROM ").append(sourceTable).append(" s");
        queryBuilder.append(" JOIN ").append(targetTable).append(" t ON ").append(joinCondition);
        queryBuilder.append(" WHERE CONVERT(date, s.").append(dateColumn).append(") = CAST(GETDATE() AS date)");

        if (exclusionCondition != null && !exclusionCondition.trim().isEmpty()) {
            queryBuilder.append(" AND ").append(exclusionCondition);
        }

        return jdbcTemplate.query(queryBuilder.toString(), new RowMapper<Map<String, Object>>() {
            @Override
            public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
                Map<String, Object> row = new HashMap<>();

                // Map source columns
                for (String columnName : sourceColumns) {
                    Object value = rs.getObject("s_" + columnName);
                    row.put("s_" + columnName, value);
                }

                // Map target columns
                for (String columnName : targetColumns) {
                    Object value = rs.getObject("t_" + columnName);
                    row.put("t_" + columnName, value);
                }

                return row;
            }
        });
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
        StringBuilder queryBuilder = new StringBuilder("SELECT ");
        queryBuilder.append(aggregateFunction).append("(").append(columnName).append(") AS result");
        queryBuilder.append(" FROM ").append(tableName);
        queryBuilder.append(" WHERE CONVERT(date, ").append(dateColumn).append(") = ?");

        if (exclusionCondition != null && !exclusionCondition.trim().isEmpty()) {
            queryBuilder.append(" AND ").append(exclusionCondition);
        }

        return jdbcTemplate.queryForObject(queryBuilder.toString(), BigDecimal.class, date);
    }
}
