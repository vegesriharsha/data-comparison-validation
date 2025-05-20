package com.company.datavalidation.repository;

import com.company.datavalidation.model.ComparisonConfig;
import com.company.datavalidation.model.ValidationResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ValidationResultRepository extends JpaRepository<ValidationResult, Long> {

    List<ValidationResult> findByComparisonConfig(ComparisonConfig comparisonConfig);

    List<ValidationResult> findByComparisonConfigId(Long comparisonConfigId);

    Page<ValidationResult> findByComparisonConfigOrderByExecutionDateDesc(ComparisonConfig comparisonConfig, Pageable pageable);

    @Query("SELECT vr FROM ValidationResult vr WHERE vr.comparisonConfig.id = :configId " +
            "AND vr.success = true ORDER BY vr.executionDate DESC")
    Optional<ValidationResult> findLatestSuccessfulByComparisonConfigId(@Param("configId") Long configId);

    List<ValidationResult> findByExecutionDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    List<ValidationResult> findByComparisonConfigIdAndExecutionDateBetween(
            Long comparisonConfigId, LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT vr FROM ValidationResult vr WHERE vr.comparisonConfig.tableName = :tableName " +
            "ORDER BY vr.executionDate DESC")
    Page<ValidationResult> findByTableName(@Param("tableName") String tableName, Pageable pageable);

    @Query("SELECT COUNT(vr) FROM ValidationResult vr WHERE vr.executionDate >= :startDate AND vr.success = :success")
    Long countBySuccessAndExecutionDateAfter(@Param("success") boolean success, @Param("startDate") LocalDateTime startDate);
}
