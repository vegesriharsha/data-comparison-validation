package com.company.datavalidation.repository;

import com.company.datavalidation.model.ColumnComparisonConfig;
import com.company.datavalidation.model.CrossTableConfig;
import com.company.datavalidation.model.DayOverDayConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ColumnComparisonConfigRepository extends JpaRepository<ColumnComparisonConfig, Long> {

    List<ColumnComparisonConfig> findByDayOverDayConfig(DayOverDayConfig dayOverDayConfig);

    List<ColumnComparisonConfig> findByCrossTableConfig(CrossTableConfig crossTableConfig);

    List<ColumnComparisonConfig> findByDayOverDayConfigId(Long dayOverDayConfigId);

    List<ColumnComparisonConfig> findByCrossTableConfigId(Long crossTableConfigId);
}
