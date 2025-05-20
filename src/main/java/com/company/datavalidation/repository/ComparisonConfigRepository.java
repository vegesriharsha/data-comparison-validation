package com.company.datavalidation.repository;

import com.company.datavalidation.model.ComparisonConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComparisonConfigRepository extends JpaRepository<ComparisonConfig, Long> {

    List<ComparisonConfig> findByEnabled(boolean enabled);

    Optional<ComparisonConfig> findByTableNameIgnoreCase(String tableName);

    @Query("SELECT c FROM ComparisonConfig c WHERE c.enabled = true AND EXISTS " +
            "(SELECT d FROM DayOverDayConfig d WHERE d.comparisonConfig = c AND d.enabled = true)")
    List<ComparisonConfig> findAllEnabledWithDayOverDayConfig();

    @Query("SELECT c FROM ComparisonConfig c WHERE c.enabled = true AND EXISTS " +
            "(SELECT ct FROM CrossTableConfig ct WHERE ct.sourceComparisonConfig = c AND ct.enabled = true)")
    List<ComparisonConfig> findAllEnabledWithCrossTableConfig();
}
