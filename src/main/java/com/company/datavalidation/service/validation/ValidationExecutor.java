package com.company.datavalidation.service.validation;

import com.company.datavalidation.model.ComparisonConfig;
import com.company.datavalidation.model.CrossTableConfig;
import com.company.datavalidation.model.DayOverDayConfig;
import com.company.datavalidation.model.ValidationResult;
import com.company.datavalidation.repository.ComparisonConfigRepository;
import com.company.datavalidation.repository.CrossTableConfigRepository;
import com.company.datavalidation.repository.DayOverDayConfigRepository;
import com.company.datavalidation.repository.ValidationResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ValidationExecutor {

    private final ComparisonConfigRepository comparisonConfigRepository;
    private final DayOverDayConfigRepository dayOverDayConfigRepository;
    private final CrossTableConfigRepository crossTableConfigRepository;
    private final ThresholdValidator thresholdValidator;
    private final ValidationResultRepository validationResultRepository;

    /**
     * Execute all enabled validations
     * @return List of validation results
     */
    public List<ValidationResult> executeAllValidations() {
        log.info("Starting execution of all enabled validations");

        // Get all enabled comparison configs
        List<ComparisonConfig> enabledConfigs = comparisonConfigRepository.findByEnabled(true);
        log.debug("Found {} enabled comparison configurations", enabledConfigs.size());

        // Execute validations for each config using virtual threads
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<List<ValidationResult>>> futures = new ArrayList<>();

            for (ComparisonConfig config : enabledConfigs) {
                CompletableFuture<List<ValidationResult>> future = CompletableFuture.supplyAsync(
                        () -> executeValidation(config), executor);
                futures.add(future);
            }

            // Combine all results
            List<ValidationResult> allResults = futures.stream()
                    .map(CompletableFuture::join)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());

            log.info("Completed execution of all enabled validations. Total: {}", allResults.size());
            return allResults;
        }
    }

    /**
     * Execute validation for a specific table
     * @param tableName Table name
     * @return List of validation results
     */
    public List<ValidationResult> executeValidationForTable(String tableName) {
        log.info("Starting execution of validations for table: {}", tableName);

        return comparisonConfigRepository.findByTableNameIgnoreCase(tableName)
                .map(this::executeValidation)
                .orElseGet(() -> {
                    log.warn("No comparison configuration found for table: {}", tableName);
                    return List.of();
                });
    }

    /**
     * Execute validation for a specific configuration
     * @param configId Configuration ID
     * @return List of validation results
     */
    public List<ValidationResult> executeValidationForConfig(Long configId) {
        log.info("Starting execution of validations for config ID: {}", configId);

        return comparisonConfigRepository.findById(configId)
                .map(this::executeValidation)
                .orElseGet(() -> {
                    log.warn("No comparison configuration found for ID: {}", configId);
                    return List.of();
                });
    }

    /**
     * Execute validation for a comparison configuration
     * @param config Comparison configuration
     * @return List of validation results
     */
    private List<ValidationResult> executeValidation(ComparisonConfig config) {
        List<ValidationResult> results = new ArrayList<>();

        if (!config.isEnabled()) {
            log.info("Skipping validation for disabled config: {}", config.getId());
            return results;
        }

        // Execute day-over-day validations
        dayOverDayConfigRepository.findByComparisonConfigId(config.getId())
                .filter(DayOverDayConfig::isEnabled)
                .ifPresent(dayOverDayConfig -> {
                    try {
                        ValidationResult result = thresholdValidator.validateDayOverDay(dayOverDayConfig);
                        results.add(result);
                    } catch (Exception e) {
                        log.error("Error executing day-over-day validation for config: {}", config.getId(), e);
                        ValidationResult result = ValidationResult.builder()
                                .comparisonConfig(config)
                                .success(false)
                                .errorMessage("Error: " + e.getMessage())
                                .executionDate(LocalDateTime.now())
                                .build();
                        results.add(result);
                    }
                });

        // Execute cross-table validations using virtual threads for parallel processing
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CrossTableConfig> crossTableConfigs = crossTableConfigRepository
                    .findBySourceComparisonConfigAndEnabled(config, true);

            if (!crossTableConfigs.isEmpty()) {
                log.debug("Found {} enabled cross-table configurations", crossTableConfigs.size());

                // Execute validations in parallel using virtual threads
                List<CompletableFuture<ValidationResult>> crossTableFutures =
                        crossTableConfigs.stream()
                                .map(crossTableConfig -> CompletableFuture.supplyAsync(() -> {
                                    try {
                                        return thresholdValidator.validateCrossTable(crossTableConfig);
                                    } catch (Exception e) {
                                        log.error("Error executing cross-table validation for config: {} and target table: {}",
                                                config.getId(), crossTableConfig.getTargetTableName(), e);
                                        return ValidationResult.builder()
                                                .comparisonConfig(config)
                                                .success(false)
                                                .errorMessage("Error: " + e.getMessage())
                                                .executionDate(LocalDateTime.now())
                                                .build();
                                    }
                                }, executor))
                                .toList();

                // Collect cross-table validation results
                List<ValidationResult> crossTableResults = crossTableFutures.stream()
                        .map(CompletableFuture::join)
                        .toList();

                results.addAll(crossTableResults);
            }
        }

        return results;
    }

    /**
     * Retry a failed validation
     * @param validationResultId Validation result ID
     * @return New validation result
     */
    public ValidationResult retryValidation(Long validationResultId) {
        log.info("Retrying validation for result ID: {}", validationResultId);

        // Get the failed validation result
        return validationResultRepository.findById(validationResultId)
                .map(failedResult -> {
                    ComparisonConfig config = failedResult.getComparisonConfig();

                    // Execute validation for this config
                    List<ValidationResult> results = executeValidation(config);

                    return results.isEmpty() ? null : results.getFirst();
                })
                .orElseGet(() -> {
                    log.warn("No validation result found for ID: {}", validationResultId);
                    return null;
                });
    }
}
