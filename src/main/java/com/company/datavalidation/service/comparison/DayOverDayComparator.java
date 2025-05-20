package com.company.datavalidation.service.comparison;

import com.company.datavalidation.model.*;
import com.company.datavalidation.repository.DynamicTableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DayOverDayComparator extends AbstractComparator {

    @Autowired
    public DayOverDayComparator(DynamicTableRepository dynamicTableRepository) {
        super(dynamicTableRepository);
    }

    /**
     * Perform day-over-day comparison
     * @param config Day-over-day configuration
     * @param columnConfigs List of column configuration
     * @param thresholdConfigs List of threshold configurations
     * @return List of validation detail results
     */
    public List<ValidationDetailResult> compare(DayOverDayConfig config,
                                                List<ColumnComparisonConfig> columnConfigs,
                                                Map<Long, ThresholdConfig> thresholdConfigs) {

        ComparisonConfig comparisonConfig = config.getComparisonConfig();
        String tableName = comparisonConfig.getTableName();
        String exclusionCondition = config.getExclusionCondition();

        // Extract column names for query
        List<String> columnNames = columnConfigs.stream()
                .map(ColumnComparisonConfig::getColumnName)
                .collect(Collectors.toList());

        // Add a date column - assuming a standard column name for date
        String dateColumn = "created_date"; // This should be configurable

        // Get today's data
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> todayData = dynamicTableRepository.getDataForDate(
                tableName, columnNames, dateColumn, today, exclusionCondition);

        // Get yesterday's data
        LocalDate yesterday = today.minusDays(1);
        List<Map<String, Object>> yesterdayData = dynamicTableRepository.getDataForDate(
                tableName, columnNames, dateColumn, yesterday, exclusionCondition);

        // Perform comparison for each column configuration
        List<ValidationDetailResult> results = new ArrayList<>();

        for (ColumnComparisonConfig columnConfig : columnConfigs) {
            String columnName = columnConfig.getColumnName();

            // Get threshold config
            ThresholdConfig thresholdConfig = thresholdConfigs.get(columnConfig.getId());
            if (thresholdConfig == null) {
                // Skip comparison if no threshold is defined
                continue;
            }

            // Perform aggregate comparison if needed
            if (columnName.contains("(") && columnName.contains(")")) {
                // This is an aggregate function
                ValidationDetailResult result = compareAggregate(
                        tableName, columnName, dateColumn, today, yesterday,
                        exclusionCondition, columnConfig, thresholdConfig);
                results.add(result);
            } else {
                // This is a regular column comparison
                List<ValidationDetailResult> columnResults = compareRegularColumn(
                        todayData, yesterdayData, columnName, columnConfig, thresholdConfig);
                results.addAll(columnResults);
            }
        }

        return results;
    }

    /**
     * Compare an aggregate column
     * @param tableName Table name
     * @param aggregateColumn Aggregate column expression (e.g., "SUM(column_name)")
     * @param dateColumn Date column name
     * @param today Today's date
     * @param yesterday Yesterday's date
     * @param exclusionCondition Exclusion condition
     * @param columnConfig Column configuration
     * @param thresholdConfig Threshold configuration
     * @return Validation detail result
     */
    private ValidationDetailResult compareAggregate(String tableName, String aggregateColumn,
                                                    String dateColumn, LocalDate today,
                                                    LocalDate yesterday, String exclusionCondition,
                                                    ColumnComparisonConfig columnConfig,
                                                    ThresholdConfig thresholdConfig) {

        // Extract aggregate function and column name
        String[] parts = aggregateColumn.split("[()]");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid aggregate column format: " + aggregateColumn);
        }

        String aggregateFunction = parts[0];
        String columnName = parts[1];

        // Get today's aggregate value
        BigDecimal todayValue = dynamicTableRepository.executeAggregateQuery(
                tableName, aggregateFunction, columnName, dateColumn, today, exclusionCondition);

        // Get yesterday's aggregate value
        BigDecimal yesterdayValue = dynamicTableRepository.executeAggregateQuery(
                tableName, aggregateFunction, columnName, dateColumn, yesterday, exclusionCondition);

        // Compare values
        ComparisonResult comparisonResult = compareValues(
                todayValue, yesterdayValue, columnConfig.getComparisonType());

        // Check if threshold is exceeded
        boolean thresholdExceeded = isThresholdExceeded(
                comparisonResult, columnConfig, thresholdConfig.getThresholdValue());

        // Create result
        ValidationDetailResult result = new ValidationDetailResult();
        result.setColumnComparisonConfig(columnConfig);
        result.setActualValue(comparisonResult.getActualValue());
        result.setExpectedValue(comparisonResult.getExpectedValue());
        result.setDifferenceValue(comparisonResult.getDifferenceValue());
        result.setDifferencePercentage(comparisonResult.getDifferencePercentage());
        result.setThresholdExceeded(thresholdExceeded);

        return result;
    }

    /**
     * Compare a regular column
     * @param todayData Today's data
     * @param yesterdayData Yesterday's data
     * @param columnName Column name
     * @param columnConfig Column configuration
     * @param thresholdConfig Threshold configuration
     * @return List of validation detail results
     */
    private List<ValidationDetailResult> compareRegularColumn(List<Map<String, Object>> todayData,
                                                              List<Map<String, Object>> yesterdayData,
                                                              String columnName,
                                                              ColumnComparisonConfig columnConfig,
                                                              ThresholdConfig thresholdConfig) {

        List<ValidationDetailResult> results = new ArrayList<>();

        // For now, just compare the sum of values
        // In a real implementation, we would need a way to match rows between today and yesterday
        BigDecimal todaySum = BigDecimal.ZERO;
        for (Map<String, Object> row : todayData) {
            BigDecimal value = extractValue(row, columnName, columnConfig.getNullHandlingStrategy());
            if (value != null) {
                todaySum = todaySum.add(value);
            }
        }

        BigDecimal yesterdaySum = BigDecimal.ZERO;
        for (Map<String, Object> row : yesterdayData) {
            BigDecimal value = extractValue(row, columnName, columnConfig.getNullHandlingStrategy());
            if (value != null) {
                yesterdaySum = yesterdaySum.add(value);
            }
        }

        // Compare values
        ComparisonResult comparisonResult = compareValues(
                todaySum, yesterdaySum, columnConfig.getComparisonType());

        // Check if threshold is exceeded
        boolean thresholdExceeded = isThresholdExceeded(
                comparisonResult, columnConfig, thresholdConfig.getThresholdValue());

        // Create result
        ValidationDetailResult result = new ValidationDetailResult();
        result.setColumnComparisonConfig(columnConfig);
        result.setActualValue(comparisonResult.getActualValue());
        result.setExpectedValue(comparisonResult.getExpectedValue());
        result.setDifferenceValue(comparisonResult.getDifferenceValue());
        result.setDifferencePercentage(comparisonResult.getDifferencePercentage());
        result.setThresholdExceeded(thresholdExceeded);

        results.add(result);

        return results;
    }
}
