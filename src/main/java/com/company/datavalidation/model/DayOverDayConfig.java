package com.company.datavalidation.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "day_over_day_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = "id")
public class DayOverDayConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comparison_config_id", nullable = false)
    @ToString.Exclude
    private ComparisonConfig comparisonConfig;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(name = "exclusion_condition")
    private String exclusionCondition;
}
