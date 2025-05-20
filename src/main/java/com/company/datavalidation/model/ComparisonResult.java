package com.company.datavalidation.model;

import java.math.BigDecimal;

/**
 * Record to hold the result of a comparison
 */
public record ComparisonResult(
        BigDecimal actualValue,
        BigDecimal expectedValue,
        BigDecimal differenceValue,
        BigDecimal differencePercentage
) {
    // Static factory method for easy creation
    public static ComparisonResult create(
            BigDecimal actualValue,
            BigDecimal expectedValue,
            BigDecimal differenceValue,
            BigDecimal differencePercentage) {
        return new ComparisonResult(actualValue, expectedValue, differenceValue, differencePercentage);
    }

    // Empty builder method to match the original pattern
    public static Builder builder() {
        return new Builder();
    }

    // Builder class for backward compatibility with code that might use the builder pattern
    public static class Builder {
        private BigDecimal actualValue;
        private BigDecimal expectedValue;
        private BigDecimal differenceValue;
        private BigDecimal differencePercentage;

        public Builder actualValue(BigDecimal actualValue) {
            this.actualValue = actualValue;
            return this;
        }

        public Builder expectedValue(BigDecimal expectedValue) {
            this.expectedValue = expectedValue;
            return this;
        }

        public Builder differenceValue(BigDecimal differenceValue) {
            this.differenceValue = differenceValue;
            return this;
        }

        public Builder differencePercentage(BigDecimal differencePercentage) {
            this.differencePercentage = differencePercentage;
            return this;
        }

        public ComparisonResult build() {
            return new ComparisonResult(actualValue, expectedValue, differenceValue, differencePercentage);
        }
    }
}
