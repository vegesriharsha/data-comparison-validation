package com.company.datavalidation.service.validation;

import com.company.datavalidation.model.*;
import com.company.datavalidation.repository.*;
import com.company.datavalidation.service.comparison.CrossTableComparator;
import com.company.datavalidation.service.comparison.DayOverDayComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ThresholdValidator {

    private static final Logger logger = LoggerFactory.getLogger(ThresholdValidator.class);

    private final DayOverDayComparator dayOverDayComparator;
    private final CrossTableComparator crossTableComparator;
    private final ColumnComparisonConfigRepository columnComparisonConfigRepository;
    private final ThresholdConfigRepository thresholdConfigRepository;
    private final ValidationResultRepository validationResultRepository;
    private final ValidationDetailResultRepository validationDetailResultRepository;

    @Autowired
    public ThresholdValidator(
            DayOverDayComparator dayOverDayComparator,
            CrossTableComparator crossTableComparator,
            ColumnComparisonConfigRepository columnComparisonConfigRepository,
            ThresholdConfigRepository thresholdConfigRepository,
            ValidationResultRepository validationResultRepository,
            ValidationDetailResultRepository validationDetailResultRepository) {
        this.dayOverDayComparator = dayOverDayComparator;
        this.crossTableComparator = crossTableComparator;
        this.columnComparisonConfigRepository = columnComparisonConfigRepository;
        this.thresholdConfigRepository = thresholdConfigRepository;
        this.validationResultRepository = validationResultRepository;
        this.validationDetailResultRepository = validationDetailResultRepository;
    }

    /**
     * Validate a day-over-day configuration
     * @param config Day-over-day configuration
     * @return Validation result
     */
    @Transactional
    public ValidationResult validateDayOverDay(DayOverDayConfig config) {
        ValidationResult result = new ValidationResult();
        result.setComparisonConfig(config.getComparisonConfig());

        long startTime = System.currentTimeMillis();

        try {
            // Get column configurations
            List<ColumnComparisonConfig> columnConfigs = columnComparisonConfigRepository.findByDayOverDayConfig(config);

            // Get threshold configurations
            Map<Long, ThresholdConfig> thresholdConfigs = getThresholdConfigs(columnConfigs);

            // Perform comparison
            List<ValidationDetailResult> detailResults = dayOverDayComparator.compare(config, columnConfigs, thresholdConfigs);

            // Check if any thresholds were exceeded
            boolean anyThresholdExceeded = detailResults.stream()
                    .anyMatch(ValidationDetailResult::isThresholdExceeded);

            // Save validation result
            result.setSuccess(!anyThresholdExceeded);
            result = validationResultRepository.save(result);

            // Link detail results to validation result
            for (ValidationDetailResult detailResult : detailResults) {
                detailResult.setValidationResult(result);
                validationDetailResultRepository.save(detailResult);
            }

        } catch (Exception e) {
            logger.error("Error validating day-over-day config: " + config.getId(), e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            result = validationResultRepository.save(result);
        }

        // Record execution time
        long endTime = System.currentTimeMillis();
        result.setExecutionTimeMs((int) (endTime - startTime));

        return validationResultRepository.save(result);
    }

    /**
     * Validate a cross-table configuration
     * @param config Cross-table configuration
     * @return Validation result
     */
    @Transactional
    public ValidationResult validateCrossTable(CrossTableConfig config) {
        ValidationResult result = new ValidationResult();
        result.setComparisonConfig(config.getSourceComparisonConfig());

        long startTime = System.currentTimeMillis();

        try {
            // Get column configurations
            List<ColumnComparisonConfig> columnConfigs = columnComparisonConfigRepository.findByCrossTableConfig(config);

            // Get threshold configurations
            Map<Long, ThresholdConfig> thresholdConfigs = getThresholdConfigs(columnConfigs);

            // Perform comparison
            List<ValidationDetailResult> detailResults = crossTableComparator.compare(config, columnConfigs, thresholdConfigs);

            // Check if any thresholds were exceeded
            boolean anyThresholdExceeded = detailResults.stream()
                    .anyMatch(ValidationDetailResult::isThresholdExceeded);

            // Save validation result
            result.setSuccess(!anyThresholdExceeded);
            result = validationResultRepository.save(result);

            // Link detail results to validation result
            for (ValidationDetailResult detailResult : detailResults) {
                detailResult.setValidationResult(result);
                validationDetailResultRepository.save(detailResult);
            }

        } catch (Exception e) {
            logger.error("Error validating cross-table config: " + config.getId(), e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            result = validationResultRepository.save(result);
        }

        // Record execution time
        long endTime = System.currentTimeMillis();
        result.setExecutionTimeMs((int) (endTime - startTime));

        return validationResultRepository.save(result);
    }

    /**
     * Get threshold configurations for column configurations
     * @param columnConfigs List of column configurations
     * @return Map of column configuration ID to threshold configuration
     */
    private Map<Long, ThresholdConfig> getThresholdConfigs(List<ColumnComparisonConfig> columnConfigs) {
        List<Long> columnConfigIds = columnConfigs.stream()
                .map(ColumnComparisonConfig::getId)
                .toList();

        List<ThresholdConfig> thresholdConfigs = new ArrayList<>();

        for (Long columnConfigId : columnConfigIds) {
            // For each column, get all thresholds
            List<ThresholdConfig> configs = thresholdConfigRepository.findByColumnComparisonConfigId(columnConfigId);
            if (!configs.isEmpty()) {
                // Just use the first threshold for now
                // In a real implementation, we might want to handle multiple thresholds per column
                thresholdConfigs.add(configs.get(0));
            }
        }

        return thresholdConfigs.stream()
                .collect(Collectors.toMap(
                        config -> config.getColumnComparisonConfig().getId(),
                        config -> config
                ));
    }
}
