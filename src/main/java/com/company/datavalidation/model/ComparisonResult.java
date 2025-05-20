package com.company.datavalidation.model;

import java.math.BigDecimal;

/**
 * Class to hold the result of a comparison
 */
public class ComparisonResult {
    private BigDecimal actualValue;
    private BigDecimal expectedValue;
    private BigDecimal differenceValue;
    private BigDecimal differencePercentage;

    public BigDecimal getActualValue() {
        return actualValue;
    }

    public void setActualValue(BigDecimal actualValue) {
        this.actualValue = actualValue;
    }

    public BigDecimal getExpectedValue() {
        return expectedValue;
    }

    public void setExpectedValue(BigDecimal expectedValue) {
        this.expectedValue = expectedValue;
    }

    public BigDecimal getDifferenceValue() {
        return differenceValue;
    }

    public void setDifferenceValue(BigDecimal differenceValue) {
        this.differenceValue = differenceValue;
    }

    public BigDecimal getDifferencePercentage() {
        return differencePercentage;
    }

    public void setDifferencePercentage(BigDecimal differencePercentage) {
        this.differencePercentage = differencePercentage;
    }
}
