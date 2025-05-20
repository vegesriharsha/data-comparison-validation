package com.company.datavalidation.service.comparison;

import com.company.datavalidation.model.*;
import com.company.datavalidation.repository.DynamicTableRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class CrossTableComparator extends AbstractComparator {

    public CrossTableComparator(DynamicTableRepository dynamicTableRepository) {
        super(dynamicTableRepository);
    }

    /**
     * Perform cross-table comparison
     * @param config Cross-table configuration
     * @param columnConfigs List of column configuration
     * @param thresholdConfigs Map of column config ID to threshold configuration
     * @return List of validation detail results
     */
    public List<ValidationDetailResult> compare(CrossTableConfig config,
                                                List<ColumnComparisonConfig> columnConfigs,
                                                Map<Long, ThresholdConfig> thresholdConfigs) {

        log.debug("Starting cross-table comparison for config: {}", config.getId());

        ComparisonConfig sourceConfig = config.getSourceComparisonConfig();
        String sourceTable = sourceConfig.getTableName();
        String targetTable = config.getTargetTableName();
        String joinCondition = config.getJoinCondition();

        // Create source and target column mapping
        record ColumnMapping(String sourceColumn, String targetColumn, ColumnComparisonConfig config) {}

        var columnMappings = columnConfigs.stream()
                .map(columnConfig -> {
                    String sourceColumn = columnConfig.getColumnName();
                    String targetColumn = columnConfig.getTargetColumnName();

                    // Use same column name if target not specified
                    if (targetColumn == null) {
                        targetColumn = sourceColumn;
                    }

                    return new ColumnMapping(sourceColumn, targetColumn, columnConfig);
                })
                .toList();

        // Extract source and target column names for query
        var sourceColumns = columnMappings.stream()
                .map(ColumnMapping::sourceColumn)
                .toList();

        var targetColumns = columnMappings.stream()
                .map(ColumnMapping::targetColumn)
                .toList();

        // Assume a standard column name for date
        String dateColumn = "created_date";

        // Execute cross-table query
        var crossTableData = dynamicTableRepository.executeCrossTableQuery(
                sourceTable, targetTable, sourceColumns, targetColumns, joinCondition,
                dateColumn, null);

        // Perform comparison for each row and column
        List<ValidationDetailResult> results = new ArrayList<>();

        for (var row : crossTableData) {
            for (var mapping : columnMappings) {
                var columnConfig = mapping.config();
                var thresholdConfig = thresholdConfigs.get(columnConfig.getId());

                if (thresholdConfig == null) {
                    log.warn("No threshold configuration found for column config: {}", columnConfig.getId());
                    continue;
                }

                // Extract source and target values
                BigDecimal sourceValue = extractValue(row, "s_" + mapping.sourceColumn(),
                        columnConfig.getNullHandlingStrategy());
                BigDecimal targetValue = extractValue(row, "t_" + mapping.targetColumn(),
                        columnConfig.getNullHandlingStrategy());

                // Skip comparison if either value is null
                if (sourceValue == null || targetValue == null) {
                    continue;
                }

                // Compare values
                ComparisonResult comparisonResult = compareValues(
                        sourceValue, targetValue, columnConfig.getComparisonType());

                // Check if threshold is exceeded
                boolean thresholdExceeded = isThresholdExceeded(
                        comparisonResult, columnConfig, thresholdConfig.getThresholdValue());

                // Create result
                var result = ValidationDetailResult.builder()
                        .columnComparisonConfig(columnConfig)
                        .actualValue(comparisonResult.actualValue())
                        .expectedValue(comparisonResult.expectedValue())
                        .differenceValue(comparisonResult.differenceValue())
                        .differencePercentage(comparisonResult.differencePercentage())
                        .thresholdExceeded(thresholdExceeded)
                        .build();

                results.add(result);
            }
        }

        return results;
    }
}
