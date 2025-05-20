package com.company.datavalidation.service.comparison;

import com.company.datavalidation.model.*;
import com.company.datavalidation.repository.DynamicTableRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractComparator {

    protected final DynamicTableRepository dynamicTableRepository;

    protected AbstractComparator(DynamicTableRepository dynamicTableRepository) {
        this.dynamicTableRepository = dynamicTableRepository;
    }

    /**
     * Compare values based on the comparison type
     * @param actual Actual value
     * @param expected Expected value
     * @param comparisonType Type of comparison
     * @return Result of the comparison
     */
    protected ComparisonResult compareValues(BigDecimal actual, BigDecimal expected, ComparisonType comparisonType) {
        ComparisonResult result = new ComparisonResult();
        result.setActualValue(actual);
        result.setExpectedValue(expected);

        if (actual == null || expected == null) {
            result.setDifferenceValue(null);
            result.setDifferencePercentage(null);
            return result;
        }

        BigDecimal differenceValue = actual.subtract(expected);
        result.setDifferenceValue(differenceValue);

        if (expected.compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal differencePercentage = differenceValue
                    .divide(expected.abs(), 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            result.setDifferencePercentage(differencePercentage);
        } else {
            // Handle division by zero
            if (actual.compareTo(BigDecimal.ZERO) == 0) {
                // Both actual and expected are zero, no difference
                result.setDifferencePercentage(BigDecimal.ZERO);
            } else {
                // Expected is zero but actual is not, set to 100% difference
                result.setDifferencePercentage(BigDecimal.valueOf(100));
            }
        }

        return result;
    }

    /**
     * Check if a value exceeds a threshold
     * @param comparisonResult Result of the comparison
     * @param columnConfig Column comparison configuration
     * @param thresholdValue Threshold value
     * @return True if threshold is exceeded, false otherwise
     */
    protected boolean isThresholdExceeded(ComparisonResult comparisonResult, ColumnComparisonConfig columnConfig, BigDecimal thresholdValue) {
        ComparisonType comparisonType = columnConfig.getComparisonType();

        return switch (comparisonType) {
            case PERCENTAGE -> {
                BigDecimal diffPercentage = comparisonResult.getDifferencePercentage();
                yield diffPercentage != null && diffPercentage.abs().compareTo(thresholdValue) > 0;
            }
            case ABSOLUTE -> {
                BigDecimal diffValue = comparisonResult.getDifferenceValue();
                yield diffValue != null && diffValue.abs().compareTo(thresholdValue) > 0;
            }
            case EXACT -> {
                // For exact comparison, any difference exceeds threshold
                BigDecimal diff = comparisonResult.getDifferenceValue();
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
            return handleNullValue(strategy);
        }

        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        } else if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        } else if (value instanceof String) {
            String strValue = (String) value;
            if (strValue.trim().isEmpty()) {
                return handleBlankValue(strategy);
            } else if (strValue.equalsIgnoreCase("N/A")) {
                return handleNAValue(strategy);
            } else {
                try {
                    return new BigDecimal(strValue);
                } catch (NumberFormatException e) {
                    // Cannot convert to number
                    return handleInvalidValue(strategy);
                }
            }
        }

        // Default case, treat as invalid
        return handleInvalidValue(strategy);
    }

    /**
     * Handle null value based on strategy
     * @param strategy Handling strategy
     * @return Handled value
     * @throws RuntimeException if strategy is FAIL
     */
    private BigDecimal handleNullValue(HandlingStrategy strategy) {
        switch (strategy) {
            case IGNORE:
                return null;
            case TREAT_AS_ZERO:
                return BigDecimal.ZERO;
            case TREAT_AS_NULL:
                return null;
            case FAIL:
                throw new RuntimeException("Null value encountered with FAIL strategy");
            default:
                return null;
        }
    }

    /**
     * Handle blank value based on strategy
     * @param strategy Handling strategy
     * @return Handled value
     * @throws RuntimeException if strategy is FAIL
     */
    private BigDecimal handleBlankValue(HandlingStrategy strategy) {
        switch (strategy) {
            case IGNORE:
                return null;
            case TREAT_AS_ZERO:
                return BigDecimal.ZERO;
            case TREAT_AS_NULL:
                return null;
            case FAIL:
                throw new RuntimeException("Blank value encountered with FAIL strategy");
            default:
                return null;
        }
    }

    /**
     * Handle N/A value based on strategy
     * @param strategy Handling strategy
     * @return Handled value
     * @throws RuntimeException if strategy is FAIL
     */
    private BigDecimal handleNAValue(HandlingStrategy strategy) {
        switch (strategy) {
            case IGNORE:
                return null;
            case TREAT_AS_ZERO:
                return BigDecimal.ZERO;
            case TREAT_AS_NULL:
                return null;
            case FAIL:
                throw new RuntimeException("N/A value encountered with FAIL strategy");
            default:
                return null;
        }
    }

    /**
     * Handle invalid value based on strategy
     * @param strategy Handling strategy
     * @return Handled value
     * @throws RuntimeException if strategy is FAIL
     */
    private BigDecimal handleInvalidValue(HandlingStrategy strategy) {
        switch (strategy) {
            case IGNORE:
                return null;
            case TREAT_AS_ZERO:
                return BigDecimal.ZERO;
            case TREAT_AS_NULL:
                return null;
            case FAIL:
                throw new RuntimeException("Invalid value encountered with FAIL strategy");
            default:
                return null;
        }
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
        for (Map<String, Object> row : rows) {
            Object rowKeyValue = row.get(keyColumn);
            if (rowKeyValue != null && rowKeyValue.equals(keyValue)) {
                return Optional.of(row);
            }
        }
        return Optional.empty();
    }
}
