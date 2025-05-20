package com.company.datavalidation.repository;

import com.company.datavalidation.model.ComparisonConfig;
import com.company.datavalidation.model.CrossTableConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CrossTableConfigRepository extends JpaRepository<CrossTableConfig, Long> {

    List<CrossTableConfig> findBySourceComparisonConfigAndEnabled(ComparisonConfig sourceComparisonConfig, boolean enabled);

    Optional<CrossTableConfig> findBySourceComparisonConfigIdAndTargetTableNameIgnoreCase(Long sourceComparisonConfigId, String targetTableName);

    List<CrossTableConfig> findByTargetTableNameIgnoreCase(String targetTableName);
}
