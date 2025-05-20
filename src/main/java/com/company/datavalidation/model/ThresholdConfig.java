package com.company.datavalidation.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "threshold_config")
public class ThresholdConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "column_comparison_config_id", nullable = false)
    private ColumnComparisonConfig columnComparisonConfig;

    @Column(name = "threshold_value", nullable = false, precision = 18, scale = 4)
    private BigDecimal thresholdValue;

    @Column(name = "severity", nullable = false)
    @Enumerated(EnumType.STRING)
    private Severity severity;

    @Column(name = "notification_enabled", nullable = false)
    private boolean notificationEnabled = true;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ColumnComparisonConfig getColumnComparisonConfig() {
        return columnComparisonConfig;
    }

    public void setColumnComparisonConfig(ColumnComparisonConfig columnComparisonConfig) {
        this.columnComparisonConfig = columnComparisonConfig;
    }

    public BigDecimal getThresholdValue() {
        return thresholdValue;
    }

    public void setThresholdValue(BigDecimal thresholdValue) {
        this.thresholdValue = thresholdValue;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public boolean isNotificationEnabled() {
        return notificationEnabled;
    }

    public void setNotificationEnabled(boolean notificationEnabled) {
        this.notificationEnabled = notificationEnabled;
    }
}
