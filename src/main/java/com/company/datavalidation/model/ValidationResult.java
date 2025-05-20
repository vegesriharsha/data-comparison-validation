package com.company.datavalidation.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "validation_result")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = "id")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ValidationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comparison_config_id", nullable = false)
    @ToString.Exclude
    // Tell Jackson how to handle this bidirectional relationship
    @JsonIgnoreProperties({"validationResults"})
    private ComparisonConfig comparisonConfig;

    @Column(name = "execution_date", nullable = false)
    @CreationTimestamp
    private LocalDateTime executionDate;

    @Column(name = "success", nullable = false)
    private boolean success;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "execution_time_ms")
    private Integer executionTimeMs;
}
