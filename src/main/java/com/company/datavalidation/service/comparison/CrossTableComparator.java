package com.company.datavalidation.service.comparison;

import com.company.datavalidation.model.*;
import com.company.datavalidation.repository.DynamicTableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CrossTableComparator extends AbstractComparator {

    @Autowired
    public CrossTableComparator(DynamicTableRepository dynamicTableRepository) {
        super(dynamicTableRepository);
    }

    /**
     * Perform cross-table comparison
     * @param config Cross-table configuration
     * @param columnConfigs List of column configuration
     * @param thresholdConfigs List of threshold configurations
     * @return List of validation detail results
     */
    public List<ValidationDetailResult> compare(CrossTableConfig config,
                                                List<ColumnComparisonConfig> columnConfigs,
                                                Map<Long, ThresholdConfig> thresholdConfigs) {

        ComparisonConfig sourceConfig = config.getSourceComparisonConfig();
        String sourceTable = sourceConfig.getTableName();
        String targetTable = config.getTargetTableName();
        String joinCondition = config.getJoinCondition();

        // Extract source and target column names for query
        List<String> sourceColumns = new ArrayList<>();
        List<String> targetColumns = new ArrayList<>();

        Map<String, ColumnComparisonConfig> columnConfigMap = new HashMap<>();

        for (ColumnComparisonConfig columnConfig : columnConfigs) {
            String sourceColumn = columnConfig.getColumnName();
            String targetColumn = columnConfig.getTargetColumnName();

            if (targetColumn == null) {
                targetColumn = sourceColumn; // Use same column name if target not specified
            }

            sourceColumns.add(sourceColumn);
            targetColumns.add(targetColumn);

            // Map source column to config
            columnConfigMap.put(sourceColumn, columnConfig);
        }

        // Assume a standard column name for date
        String dateColumn = "created_date"; // This should be configurable

        // Execute cross-table query
        List<Map<String, Object>> crossTableData = dynamicTableRepository.executeCrossTableQuery(
                sourceTable, targetTable, sourceColumns, targetColumns, joinCondition,
                dateColumn, null); // No exclusion condition for now

        // Perform comparison for each row and column
        List<ValidationDetailResult> results = new ArrayList<>();

        for (Map<String, Object> row : crossTableData) {
            for (String sourceColumn : sourceColumns) {
                int index = sourceColumns.indexOf(sourceColumn);
                String targetColumn = targetColumns.get(index);

                ColumnComparisonConfig columnConfig = columnConfigMap.get(sourceColumn);
                ThresholdConfig thresholdConfig = thresholdConfigs.get(columnConfig.getId());

                if (thresholdConfig == null) {
                    // Skip comparison if no threshold is defined
                    continue;
                }

                // Extract source and target values
                BigDecimal sourceValue = extractValue(row, "s_" + sourceColumn, columnConfig.getNullHandlingStrategy());
                BigDecimal targetValue = extractValue(row, "t_" + targetColumn, columnConfig.getNullHandlingStrategy());

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
                ValidationDetailResult result = new ValidationDetailResult();
                result.setColumnComparisonConfig(columnConfig);
                result.setActualValue(comparisonResult.getActualValue());
                result.setExpectedValue(comparisonResult.getExpectedValue());
                result.setDifferenceValue(comparisonResult.getDifferenceValue());
                result.setDifferencePercentage(comparisonResult.getDifferencePercentage());
                result.setThresholdExceeded(thresholdExceeded);

                results.add(result);
            }
        }

        return results;
    }
}
