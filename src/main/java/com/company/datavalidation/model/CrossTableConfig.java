package com.company.datavalidation.model;

import jakarta.persistence.*;

@Entity
@Table(name = "cross_table_config")
public class CrossTableConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_comparison_config_id", nullable = false)
    private ComparisonConfig sourceComparisonConfig;

    @Column(name = "target_table_name", nullable = false)
    private String targetTableName;

    @Column(name = "join_condition", nullable = false)
    private String joinCondition;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ComparisonConfig getSourceComparisonConfig() {
        return sourceComparisonConfig;
    }

    public void setSourceComparisonConfig(ComparisonConfig sourceComparisonConfig) {
        this.sourceComparisonConfig = sourceComparisonConfig;
    }

    public String getTargetTableName() {
        return targetTableName;
    }

    public void setTargetTableName(String targetTableName) {
        this.targetTableName = targetTableName;
    }

    public String getJoinCondition() {
        return joinCondition;
    }

    public void setJoinCondition(String joinCondition) {
        this.joinCondition = joinCondition;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
