package com.company.datavalidation.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.function.BiFunction;

/**
 * Comparison types with associated comparison logic
 */
@Getter
@Slf4j // Add a logger to the enum
@RequiredArgsConstructor // Creates a constructor for all final fields
@ToString // Enhances the toString method
public enum ComparisonType {

    PERCENTAGE(
            "Percentage difference",
            (actual, expected) -> {
                if (expected.compareTo(BigDecimal.ZERO) == 0) {
                    return actual.compareTo(BigDecimal.ZERO) == 0
                            ? BigDecimal.ZERO
                            : new BigDecimal("100");
                }
                return actual.subtract(expected)
                        .divide(expected.abs(), 6, java.math.RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
            }
    ),

    ABSOLUTE(
            "Absolute difference",
            (actual, expected) -> actual.subtract(expected)
    ),

    EXACT(
            "Exact match",
            (actual, expected) -> actual.compareTo(expected) == 0
                    ? BigDecimal.ZERO
                    : BigDecimal.ONE
    );

    // Only need getter, as these fields should be immutable
    private final String description;

    private final BiFunction<BigDecimal, BigDecimal, BigDecimal> calculator;

    // Constructor is handled by @RequiredArgsConstructor

    /**
     * Calculate the difference based on the comparison type
     *
     * @param actual Actual value
     * @param expected Expected value
     * @return The calculated difference
     */
    public BigDecimal calculateDifference(BigDecimal actual, BigDecimal expected) {
        if (actual == null || expected == null) {
            log.debug("Null values detected in comparison: actual={}, expected={}", actual, expected);
            return null;
        }
        log.trace("Calculating difference using {} comparison: actual={}, expected={}", this.name(), actual, expected);
        return calculator.apply(actual, expected);
    }

    /**
     * Check if the difference exceeds the threshold
     *
     * @param difference The calculated difference
     * @param threshold The threshold value
     * @return True if the threshold is exceeded
     */
    public boolean isThresholdExceeded(BigDecimal difference, BigDecimal threshold) {
        if (difference == null || threshold == null) {
            log.debug("Cannot determine threshold: difference={}, threshold={}", difference, threshold);
            return false;
        }

        boolean exceeded = switch(this) {
            case PERCENTAGE, ABSOLUTE -> difference.abs().compareTo(threshold) > 0;
            case EXACT -> difference.compareTo(BigDecimal.ZERO) != 0;
        };

        if (exceeded) {
            log.debug("Threshold exceeded for {} comparison: difference={}, threshold={}",
                    this.name(), difference, threshold);
        }

        return exceeded;
    }
}
