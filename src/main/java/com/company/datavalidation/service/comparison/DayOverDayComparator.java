package com.company.datavalidation.service.comparison;

import com.company.datavalidation.model.*;
import com.company.datavalidation.repository.DynamicTableRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DayOverDayComparator extends AbstractComparator {

    public DayOverDayComparator(DynamicTableRepository dynamicTableRepository) {
        super(dynamicTableRepository);
    }

    /**
     * Perform day-over-day comparison
     * @param config Day-over-day configuration
     * @param columnConfigs List of column configuration
     * @param thresholdConfigs Map of column config ID to threshold configuration
     * @return List of validation detail results
     */
    public List<ValidationDetailResult> compare(DayOverDayConfig config,
                                                List<ColumnComparisonConfig> columnConfigs,
                                                Map<Long, ThresholdConfig> thresholdConfigs) {

        log.debug("Starting day-over-day comparison for config: {}", config.getId());

        ComparisonConfig comparisonConfig = config.getComparisonConfig();
        String tableName = comparisonConfig.getTableName();
        String exclusionCondition = config.getExclusionCondition();

        // Extract column names for query
        List<String> columnNames = columnConfigs.stream()
                .map(ColumnComparisonConfig::getColumnName)
                .toList();

        // Add a date column - assuming a standard column name for date
        String dateColumn = "created_date"; // This should be configurable

        // Get today's data
        LocalDate today = LocalDate.now();
        var todayData = dynamicTableRepository.getDataForDate(
                tableName, columnNames, dateColumn, today, exclusionCondition);

        // Get yesterday's data
        LocalDate yesterday = today.minusDays(1);
        var yesterdayData = dynamicTableRepository.getDataForDate(
                tableName, columnNames, dateColumn, yesterday, exclusionCondition);

        // Perform comparison for each column configuration
        return columnConfigs.stream()
                .flatMap(columnConfig -> {
                    String columnName = columnConfig.getColumnName();

                    // Get threshold config
                    ThresholdConfig thresholdConfig = thresholdConfigs.get(columnConfig.getId());
                    if (thresholdConfig == null) {
                        log.warn("No threshold configuration found for column config: {}", columnConfig.getId());
                        return java.util.stream.Stream.empty();
                    }

                    // Determine comparison type
                    return columnName.contains("(") && columnName.contains(")")
                            ? java.util.stream.Stream.of(
                            compareAggregate(tableName, columnName, dateColumn, today, yesterday,
                                    exclusionCondition, columnConfig, thresholdConfig))
                            : compareRegularColumn(todayData, yesterdayData, columnName,
                            columnConfig, thresholdConfig).stream();
                })
                .collect(Collectors.toList());
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

        // Create and return result
        return ValidationDetailResult.builder()
                .columnComparisonConfig(columnConfig)
                .actualValue(comparisonResult.actualValue())
                .expectedValue(comparisonResult.expectedValue())
                .differenceValue(comparisonResult.differenceValue())
                .differencePercentage(comparisonResult.differencePercentage())
                .thresholdExceeded(thresholdExceeded)
                .build();
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

        // Sum values for today
        BigDecimal todaySum = todayData.stream()
                .map(row -> extractValue(row, columnName, columnConfig.getNullHandlingStrategy()))
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Sum values for yesterday
        BigDecimal yesterdaySum = yesterdayData.stream()
                .map(row -> extractValue(row, columnName, columnConfig.getNullHandlingStrategy()))
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Compare values
        ComparisonResult comparisonResult = compareValues(
                todaySum, yesterdaySum, columnConfig.getComparisonType());

        // Check if threshold is exceeded
        boolean thresholdExceeded = isThresholdExceeded(
                comparisonResult, columnConfig, thresholdConfig.getThresholdValue());

        // Create and return result
        return List.of(ValidationDetailResult.builder()
                .columnComparisonConfig(columnConfig)
                .actualValue(comparisonResult.actualValue())
                .expectedValue(comparisonResult.expectedValue())
                .differenceValue(comparisonResult.differenceValue())
                .differencePercentage(comparisonResult.differencePercentage())
                .thresholdExceeded(thresholdExceeded)
                .build());
    }
}
