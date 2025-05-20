package com.company.datavalidation.repository;

import com.company.datavalidation.model.ComparisonConfig;
import com.company.datavalidation.model.DayOverDayConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DayOverDayConfigRepository extends JpaRepository<DayOverDayConfig, Long> {

    List<DayOverDayConfig> findByComparisonConfigAndEnabled(ComparisonConfig comparisonConfig, boolean enabled);

    Optional<DayOverDayConfig> findByComparisonConfigId(Long comparisonConfigId);
}
