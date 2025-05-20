package com.company.datavalidation.service.validation;

import com.company.datavalidation.model.*;
import com.company.datavalidation.repository.ColumnComparisonConfigRepository;
import com.company.datavalidation.repository.ThresholdConfigRepository;
import com.company.datavalidation.repository.ValidationDetailResultRepository;
import com.company.datavalidation.repository.ValidationResultRepository;
import com.company.datavalidation.service.comparison.CrossTableComparator;
import com.company.datavalidation.service.comparison.DayOverDayComparator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ThresholdValidator {

    private final DayOverDayComparator dayOverDayComparator;
    private final CrossTableComparator crossTableComparator;
    private final ColumnComparisonConfigRepository columnComparisonConfigRepository;
    private final ThresholdConfigRepository thresholdConfigRepository;
    private final ValidationResultRepository validationResultRepository;
    private final ValidationDetailResultRepository validationDetailResultRepository;

    /**
     * Validate a day-over-day configuration
     * @param config Day-over-day configuration
     * @return Validation result
     */
    @Transactional
    public ValidationResult validateDayOverDay(DayOverDayConfig config) {
        log.info("Validating day-over-day config: {}", config.getId());

        // Create the validation result first
        final ValidationResult result = ValidationResult.builder()
                .comparisonConfig(config.getComparisonConfig())
                .executionDate(LocalDateTime.now())
                .build();

        long startTime = System.currentTimeMillis();

        try {
            // Get column configurations
            List<ColumnComparisonConfig> columnConfigs = columnComparisonConfigRepository.findByDayOverDayConfig(config);
            log.debug("Found {} column configurations", columnConfigs.size());

            // Get threshold configurations
            Map<Long, ThresholdConfig> thresholdConfigs = getThresholdConfigs(columnConfigs);
            log.debug("Found {} threshold configurations", thresholdConfigs.size());

            // Perform comparison
            List<ValidationDetailResult> detailResults = dayOverDayComparator.compare(config, columnConfigs, thresholdConfigs);
            log.debug("Comparison generated {} detail results", detailResults.size());

            // Check if any thresholds were exceeded
            boolean anyThresholdExceeded = detailResults.stream()
                    .anyMatch(ValidationDetailResult::isThresholdExceeded);

            // Save validation result
            result.setSuccess(!anyThresholdExceeded);
            ValidationResult savedResult = validationResultRepository.save(result);
            log.debug("Saved validation result: {}, success: {}", savedResult.getId(), savedResult.isSuccess());

            // Link detail results to validation result
            final ValidationResult finalResult = savedResult; // Create final reference for lambda
            detailResults.forEach(detailResult -> {
                detailResult.setValidationResult(finalResult);
                validationDetailResultRepository.save(detailResult);
            });

            // Return the saved result
            result.setId(savedResult.getId());

        } catch (Exception e) {
            log.error("Error validating day-over-day config: {}", config.getId(), e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            validationResultRepository.save(result);
        }

        // Record execution time
        long endTime = System.currentTimeMillis();
        result.setExecutionTimeMs((int) (endTime - startTime));
        log.info("Day-over-day validation completed in {}ms", result.getExecutionTimeMs());

        return validationResultRepository.save(result);
    }

    /**
     * Validate a cross-table configuration
     * @param config Cross-table configuration
     * @return Validation result
     */
    @Transactional
    public ValidationResult validateCrossTable(CrossTableConfig config) {
        log.info("Validating cross-table config: {}", config.getId());

        // Create the validation result first
        final ValidationResult result = ValidationResult.builder()
                .comparisonConfig(config.getSourceComparisonConfig())
                .executionDate(LocalDateTime.now())
                .build();

        long startTime = System.currentTimeMillis();

        try {
            // Get column configurations
            List<ColumnComparisonConfig> columnConfigs = columnComparisonConfigRepository.findByCrossTableConfig(config);
            log.debug("Found {} column configurations", columnConfigs.size());

            // Get threshold configurations
            Map<Long, ThresholdConfig> thresholdConfigs = getThresholdConfigs(columnConfigs);
            log.debug("Found {} threshold configurations", thresholdConfigs.size());

            // Perform comparison
            List<ValidationDetailResult> detailResults = crossTableComparator.compare(config, columnConfigs, thresholdConfigs);
            log.debug("Comparison generated {} detail results", detailResults.size());

            // Check if any thresholds were exceeded
            boolean anyThresholdExceeded = detailResults.stream()
                    .anyMatch(ValidationDetailResult::isThresholdExceeded);

            // Save validation result
            result.setSuccess(!anyThresholdExceeded);
            ValidationResult savedResult = validationResultRepository.save(result);
            log.debug("Saved validation result: {}, success: {}", savedResult.getId(), savedResult.isSuccess());

            // Link detail results to validation result
            final ValidationResult finalResult = savedResult; // Create final reference for lambda
            detailResults.forEach(detailResult -> {
                detailResult.setValidationResult(finalResult);
                validationDetailResultRepository.save(detailResult);
            });

            // Return the saved result
            result.setId(savedResult.getId());

        } catch (Exception e) {
            log.error("Error validating cross-table config: {}", config.getId(), e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            validationResultRepository.save(result);
        }

        // Record execution time
        long endTime = System.currentTimeMillis();
        result.setExecutionTimeMs((int) (endTime - startTime));
        log.info("Cross-table validation completed in {}ms", result.getExecutionTimeMs());

        return validationResultRepository.save(result);
    }

    /**
     * Get threshold configurations for column configurations
     * @param columnConfigs List of column configurations
     * @return Map of column configuration ID to threshold configuration
     */
    private Map<Long, ThresholdConfig> getThresholdConfigs(List<ColumnComparisonConfig> columnConfigs) {
        // Extract column config IDs
        List<Long> columnConfigIds = columnConfigs.stream()
                .map(ColumnComparisonConfig::getId)
                .toList();

        // Create result map
        Map<Long, ThresholdConfig> result = new HashMap<>();

        // Process each column config ID
        for (Long columnConfigId : columnConfigIds) {
            // For each column, get all thresholds
            List<ThresholdConfig> configs = thresholdConfigRepository.findByColumnComparisonConfigId(columnConfigId);
            if (!configs.isEmpty()) {
                // Just use the first threshold for now
                // In a real implementation, we might want to handle multiple thresholds per column
                result.put(columnConfigId, configs.getFirst());
            }
        }

        return result;
    }
}
