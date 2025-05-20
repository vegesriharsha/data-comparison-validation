package com.company.datavalidation.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "validation_detail_result")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = "id")
public class ValidationDetailResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validation_result_id", nullable = false)
    @ToString.Exclude
    private ValidationResult validationResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "column_comparison_config_id", nullable = false)
    @ToString.Exclude
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
}
