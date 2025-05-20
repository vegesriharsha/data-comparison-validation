package com.company.datavalidation.service.comparison;

import com.company.datavalidation.model.ColumnComparisonConfig;
import com.company.datavalidation.model.ComparisonResult;
import com.company.datavalidation.model.ComparisonType;
import com.company.datavalidation.model.HandlingStrategy;
import com.company.datavalidation.repository.DynamicTableRepository;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
public abstract class AbstractComparator {

    protected final DynamicTableRepository dynamicTableRepository;

    /**
     * Compare values based on the comparison type
     * @param actual Actual value
     * @param expected Expected value
     * @param comparisonType Type of comparison
     * @return Result of the comparison
     */
    protected ComparisonResult compareValues(BigDecimal actual, BigDecimal expected, ComparisonType comparisonType) {
        // Handle null values
        if (actual == null || expected == null) {
            return ComparisonResult.builder()
                    .actualValue(actual)
                    .expectedValue(expected)
                    .differenceValue(null)
                    .differencePercentage(null)
                    .build();
        }

        // Calculate difference
        BigDecimal differenceValue = actual.subtract(expected);

        // Calculate percentage difference
        BigDecimal differencePercentage = null;
        if (expected.compareTo(BigDecimal.ZERO) != 0) {
            differencePercentage = differenceValue
                    .divide(expected.abs(), 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        } else {
            // Handle division by zero
            differencePercentage = actual.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO  // Both are zero, no difference
                    : BigDecimal.valueOf(100);  // Expected is zero but actual isn't
        }

        return ComparisonResult.builder()
                .actualValue(actual)
                .expectedValue(expected)
                .differenceValue(differenceValue)
                .differencePercentage(differencePercentage)
                .build();
    }

    /**
     * Check if a value exceeds a threshold
     * @param comparisonResult Result of the comparison
     * @param columnConfig Column comparison configuration
     * @param thresholdValue Threshold value
     * @return True if threshold is exceeded, false otherwise
     */
    protected boolean isThresholdExceeded(ComparisonResult comparisonResult, ColumnComparisonConfig columnConfig, BigDecimal thresholdValue) {
        return switch (columnConfig.getComparisonType()) {
            case PERCENTAGE -> {
                BigDecimal diffPercentage = comparisonResult.differencePercentage();
                yield diffPercentage != null && diffPercentage.abs().compareTo(thresholdValue) > 0;
            }
            case ABSOLUTE -> {
                BigDecimal diffValue = comparisonResult.differenceValue();
                yield diffValue != null && diffValue.abs().compareTo(thresholdValue) > 0;
            }
            case EXACT -> {
                // For exact comparison, any difference exceeds threshold
                BigDecimal diff = comparisonResult.differenceValue();
                yield diff != null && diff.compareTo(BigDecimal.ZERO) != 0;
            }
        };
    }

    /**
     * Handle value based on handling strategy
     * @param value Value to handle
     * @param strategy Handling strategy
     * @return Handled value
     * @throws RuntimeException if strategy is FAIL
     */
    protected BigDecimal handleValue(Object value, HandlingStrategy strategy) {
        if (value == null) {
            return strategy.handleValue(null);
        }

        return switch (value) {
            case BigDecimal bd -> bd;
            case Number n -> BigDecimal.valueOf(n.doubleValue());
            case String s -> {
                if (s.trim().isEmpty()) {
                    yield strategy.handleValue("blank");
                } else if (s.equalsIgnoreCase("N/A")) {
                    yield strategy.handleValue("N/A");
                } else {
                    try {
                        yield new BigDecimal(s);
                    } catch (NumberFormatException e) {
                        yield strategy.handleValue("invalid: " + s);
                    }
                }
            }
            default -> strategy.handleValue("unsupported: " + value.getClass().getSimpleName());
        };
    }

    /**
     * Extract value from a row
     * @param row Row data
     * @param columnName Column name
     * @param strategy Handling strategy for special values
     * @return Extracted value
     */
    protected BigDecimal extractValue(Map<String, Object> row, String columnName, HandlingStrategy strategy) {
        Object value = row.get(columnName);
        return handleValue(value, strategy);
    }

    /**
     * Find matching row in a list of rows based on a key
     * @param rows List of rows
     * @param keyColumn Key column name
     * @param keyValue Key value
     * @return Optional containing the matching row, or empty if not found
     */
    protected Optional<Map<String, Object>> findMatchingRow(Iterable<Map<String, Object>> rows, String keyColumn, Object keyValue) {
        if (keyValue == null) {
            return Optional.empty();
        }

        for (Map<String, Object> row : rows) {
            Object rowKeyValue = row.get(keyColumn);
            if (rowKeyValue != null && rowKeyValue.equals(keyValue)) {
                return Optional.of(row);
            }
        }
        return Optional.empty();
    }
}
