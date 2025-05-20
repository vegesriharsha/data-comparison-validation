package com.company.datavalidation.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "column_comparison_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = "id")
public class ColumnComparisonConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "day_over_day_config_id")
    @ToString.Exclude
    private DayOverDayConfig dayOverDayConfig;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cross_table_config_id")
    @ToString.Exclude
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
}
