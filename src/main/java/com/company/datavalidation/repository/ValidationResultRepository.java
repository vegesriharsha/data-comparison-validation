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

/**
 * Repository for accessing ValidationResult entities
 */
@Repository
public interface ValidationResultRepository extends JpaRepository<ValidationResult, Long> {

    /**
     * Find all validation results for a comparison config
     */
    List<ValidationResult> findByComparisonConfig(ComparisonConfig comparisonConfig);

    /**
     * Find all validation results for a comparison config by ID
     */
    List<ValidationResult> findByComparisonConfigId(Long comparisonConfigId);

    /**
     * Find validation results for a comparison config with pagination
     */
    Page<ValidationResult> findByComparisonConfigOrderByExecutionDateDesc(ComparisonConfig comparisonConfig, Pageable pageable);

    /**
     * Find the latest successful validation for a comparison config
     */
    @Query("""
        SELECT vr FROM ValidationResult vr 
        WHERE vr.comparisonConfig.id = :configId 
        AND vr.success = true 
        ORDER BY vr.executionDate DESC
        """)
    Optional<ValidationResult> findLatestSuccessfulByComparisonConfigId(@Param("configId") Long configId);

    /**
     * Find all validation results executed between a date range
     */
    List<ValidationResult> findByExecutionDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find all validation results for a specific config executed between a date range
     */
    List<ValidationResult> findByComparisonConfigIdAndExecutionDateBetween(
            Long comparisonConfigId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find all validation results for a specific table with pagination
     */
    @Query("""
        SELECT vr FROM ValidationResult vr 
        WHERE vr.comparisonConfig.tableName = :tableName 
        ORDER BY vr.executionDate DESC
        """)
    Page<ValidationResult> findByTableName(@Param("tableName") String tableName, Pageable pageable);

    /**
     * Count validation results by success status after a specified date
     */
    @Query("""
        SELECT COUNT(vr) FROM ValidationResult vr 
        WHERE vr.executionDate >= :startDate 
        AND vr.success = :success
        """)
    Long countBySuccessAndExecutionDateAfter(
            @Param("success") boolean success,
            @Param("startDate") LocalDateTime startDate);

    /**
     * Find all validation results grouped by table with success rate calculation
     */
    @Query("""
        SELECT 
            vr.comparisonConfig.tableName as tableName,
            COUNT(vr) as totalCount,
            SUM(CASE WHEN vr.success = true THEN 1 ELSE 0 END) as successCount,
            (SUM(CASE WHEN vr.success = true THEN 1 ELSE 0 END) * 100.0 / COUNT(vr)) as successRate
        FROM ValidationResult vr
        WHERE vr.executionDate >= :startDate
        GROUP BY vr.comparisonConfig.tableName
        """)
    List<TableValidationSummary> getValidationSummaryByTable(@Param("startDate") LocalDateTime startDate);

    /**
     * Projection interface for table validation summary
     */
    interface TableValidationSummary {
        String getTableName();
        Long getTotalCount();
        Long getSuccessCount();
        Double getSuccessRate();
    }
}
