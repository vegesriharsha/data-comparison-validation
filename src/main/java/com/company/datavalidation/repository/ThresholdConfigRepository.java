package com.company.datavalidation.repository;

import com.company.datavalidation.model.ColumnComparisonConfig;
import com.company.datavalidation.model.Severity;
import com.company.datavalidation.model.ThresholdConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ThresholdConfigRepository extends JpaRepository<ThresholdConfig, Long> {

    List<ThresholdConfig> findByColumnComparisonConfig(ColumnComparisonConfig columnComparisonConfig);

    List<ThresholdConfig> findByColumnComparisonConfigIdAndNotificationEnabled(Long columnComparisonConfigId, boolean notificationEnabled);

    List<ThresholdConfig> findBySeverityAndNotificationEnabled(Severity severity, boolean notificationEnabled);

    Optional<ThresholdConfig> findByColumnComparisonConfigIdAndSeverity(Long columnComparisonConfigId, Severity severity);

    List<ThresholdConfig> findByColumnComparisonConfigId(Long columnConfigId);
}
