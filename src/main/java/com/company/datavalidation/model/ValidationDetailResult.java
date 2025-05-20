package com.company.datavalidation.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "validation_detail_result")
public class ValidationDetailResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validation_result_id", nullable = false)
    private ValidationResult validationResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "column_comparison_config_id", nullable = false)
    private ColumnComparisonConfig columnComparisonConfig;

    @Column(name = "threshold_exceeded", nullable = false)
    private boolean thresholdExceeded;

    @Column(name = "actual_value", precision = 18, scale = 4)
    private BigDecimal actualValue;

    @Column(name = "expected_value", precision = 18, scale = 4)
    private BigDecimal expectedValue;

    @Column(name = "difference_value", precision = 18, scale = 4)
    private BigDecimal differenceValue;

    @Column(name = "difference_percentage", precision = 18, scale = 4)
    private BigDecimal differencePercentage;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ValidationResult getValidationResult() {
        return validationResult;
    }

    public void setValidationResult(ValidationResult validationResult) {
        this.validationResult = validationResult;
    }

    public ColumnComparisonConfig getColumnComparisonConfig() {
        return columnComparisonConfig;
    }

    public void setColumnComparisonConfig(ColumnComparisonConfig columnComparisonConfig) {
        this.columnComparisonConfig = columnComparisonConfig;
    }

    public boolean isThresholdExceeded() {
        return thresholdExceeded;
    }

    public void setThresholdExceeded(boolean thresholdExceeded) {
        this.thresholdExceeded = thresholdExceeded;
    }

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
