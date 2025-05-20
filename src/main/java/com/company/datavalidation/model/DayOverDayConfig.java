package com.company.datavalidation.model;

import jakarta.persistence.*;

@Entity
@Table(name = "day_over_day_config")
public class DayOverDayConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comparison_config_id", nullable = false)
    private ComparisonConfig comparisonConfig;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "exclusion_condition")
    private String exclusionCondition;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ComparisonConfig getComparisonConfig() {
        return comparisonConfig;
    }

    public void setComparisonConfig(ComparisonConfig comparisonConfig) {
        this.comparisonConfig = comparisonConfig;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getExclusionCondition() {
        return exclusionCondition;
    }

    public void setExclusionCondition(String exclusionCondition) {
        this.exclusionCondition = exclusionCondition;
    }
}
