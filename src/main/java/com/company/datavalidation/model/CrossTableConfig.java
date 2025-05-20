package com.company.datavalidation.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cross_table_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = "id")
public class CrossTableConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_comparison_config_id", nullable = false)
    @ToString.Exclude
    private ComparisonConfig sourceComparisonConfig;

    @Column(name = "target_table_name", nullable = false)
    private String targetTableName;

    @Column(name = "join_condition", nullable = false)
    private String joinCondition;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;
}
