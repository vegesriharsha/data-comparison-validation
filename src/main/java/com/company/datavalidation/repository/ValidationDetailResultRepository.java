package com.company.datavalidation.repository;

import com.company.datavalidation.model.ValidationDetailResult;
import com.company.datavalidation.model.ValidationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ValidationDetailResultRepository extends JpaRepository<ValidationDetailResult, Long> {

    List<ValidationDetailResult> findByValidationResult(ValidationResult validationResult);

    List<ValidationDetailResult> findByValidationResultId(Long validationResultId);

    List<ValidationDetailResult> findByValidationResultIdAndThresholdExceeded(Long validationResultId, boolean thresholdExceeded);

    @Query("SELECT vdr FROM ValidationDetailResult vdr WHERE vdr.validationResult.comparisonConfig.id = :configId " +
            "AND vdr.thresholdExceeded = true ORDER BY vdr.validationResult.executionDate DESC")
    List<ValidationDetailResult> findFailedValidationsByComparisonConfigId(@Param("configId") Long configId);

    @Query("SELECT COUNT(vdr) FROM ValidationDetailResult vdr WHERE vdr.validationResult.id = :resultId " +
            "AND vdr.thresholdExceeded = true")
    Long countFailedValidationsByResultId(@Param("resultId") Long resultId);
}
