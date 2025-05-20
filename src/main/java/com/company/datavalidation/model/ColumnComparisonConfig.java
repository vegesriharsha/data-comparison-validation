package com.company.datavalidation.model;

import jakarta.persistence.*;

@Entity
@Table(name = "column_comparison_config")
public class ColumnComparisonConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "day_over_day_config_id")
    private DayOverDayConfig dayOverDayConfig;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cross_table_config_id")
    private CrossTableConfig crossTableConfig;

    @Column(name = "column_name", nullable = false)
    private String columnName;

    @Column(name = "target_column_name")
    private String targetColumnName;

    @Column(name = "comparison_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ComparisonType comparisonType;

    @Column(name = "null_handling_strategy", nullable = false)
    @Enumerated(EnumType.STRING)
    private HandlingStrategy nullHandlingStrategy;

    @Column(name = "blank_handling_strategy", nullable = false)
    @Enumerated(EnumType.STRING)
    private HandlingStrategy blankHandlingStrategy;

    @Column(name = "na_handling_strategy", nullable = false)
    @Enumerated(EnumType.STRING)
    private HandlingStrategy naHandlingStrategy;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DayOverDayConfig getDayOverDayConfig() {
        return dayOverDayConfig;
    }

    public void setDayOverDayConfig(DayOverDayConfig dayOverDayConfig) {
        this.dayOverDayConfig = dayOverDayConfig;
    }

    public CrossTableConfig getCrossTableConfig() {
        return crossTableConfig;
    }

    public void setCrossTableConfig(CrossTableConfig crossTableConfig) {
        this.crossTableConfig = crossTableConfig;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getTargetColumnName() {
        return targetColumnName;
    }

    public void setTargetColumnName(String targetColumnName) {
        this.targetColumnName = targetColumnName;
    }

    public ComparisonType getComparisonType() {
        return comparisonType;
    }

    public void setComparisonType(ComparisonType comparisonType) {
        this.comparisonType = comparisonType;
    }

    public HandlingStrategy getNullHandlingStrategy() {
        return nullHandlingStrategy;
    }

    public void setNullHandlingStrategy(HandlingStrategy nullHandlingStrategy) {
        this.nullHandlingStrategy = nullHandlingStrategy;
    }

    public HandlingStrategy getBlankHandlingStrategy() {
        return blankHandlingStrategy;
    }

    public void setBlankHandlingStrategy(HandlingStrategy blankHandlingStrategy) {
        this.blankHandlingStrategy = blankHandlingStrategy;
    }

    public HandlingStrategy getNaHandlingStrategy() {
        return naHandlingStrategy;
    }

    public void setNaHandlingStrategy(HandlingStrategy naHandlingStrategy) {
        this.naHandlingStrategy = naHandlingStrategy;
    }
}
