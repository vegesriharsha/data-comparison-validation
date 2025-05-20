package com.company.datavalidation.service.validation;

import com.company.datavalidation.model.*;
import com.company.datavalidation.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class ValidationExecutor {

    private static final Logger logger = LoggerFactory.getLogger(ValidationExecutor.class);

    private final ComparisonConfigRepository comparisonConfigRepository;
    private final DayOverDayConfigRepository dayOverDayConfigRepository;
    private final CrossTableConfigRepository crossTableConfigRepository;
    private final ThresholdValidator thresholdValidator;

    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    @Autowired
    public ValidationExecutor(
            ComparisonConfigRepository comparisonConfigRepository,
            DayOverDayConfigRepository dayOverDayConfigRepository,
            CrossTableConfigRepository crossTableConfigRepository,
            ThresholdValidator thresholdValidator) {
        this.comparisonConfigRepository = comparisonConfigRepository;
        this.dayOverDayConfigRepository = dayOverDayConfigRepository;
        this.crossTableConfigRepository = crossTableConfigRepository;
        this.thresholdValidator = thresholdValidator;
    }

    /**
     * Execute all enabled validations
     * @return List of validation results
     */
    public List<ValidationResult> executeAllValidations() {
        logger.info("Starting execution of all enabled validations");

        // Get all enabled comparison configs
        List<ComparisonConfig> enabledConfigs = comparisonConfigRepository.findByEnabled(true);

        // Execute validations for each config
        List<CompletableFuture<List<ValidationResult>>> futures = new ArrayList<>();

        for (ComparisonConfig config : enabledConfigs) {
            CompletableFuture<List<ValidationResult>> future = CompletableFuture.supplyAsync(
                    () -> executeValidation(config), executorService);
            futures.add(future);
        }

        // Combine all results
        List<ValidationResult> allResults = futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .collect(Collectors.toList());

        logger.info("Completed execution of all enabled validations. Total: {}", allResults.size());

        return allResults;
    }

    /**
     * Execute validation for a specific table
     * @param tableName Table name
     * @return List of validation results
     */
    public List<ValidationResult> executeValidationForTable(String tableName) {
        logger.info("Starting execution of validations for table: {}", tableName);

        Optional<ComparisonConfig> configOpt = comparisonConfigRepository.findByTableNameIgnoreCase(tableName);

        if (configOpt.isPresent()) {
            ComparisonConfig config = configOpt.get();
            List<ValidationResult> results = executeValidation(config);
            logger.info("Completed execution of validations for table: {}. Total: {}", tableName, results.size());
            return results;
        } else {
            logger.warn("No comparison configuration found for table: {}", tableName);
            return new ArrayList<>();
        }
    }

    /**
     * Execute validation for a specific configuration
     * @param configId Configuration ID
     * @return List of validation results
     */
    public List<ValidationResult> executeValidationForConfig(Long configId) {
        logger.info("Starting execution of validations for config ID: {}", configId);

        Optional<ComparisonConfig> configOpt = comparisonConfigRepository.findById(configId);

        if (configOpt.isPresent()) {
            ComparisonConfig config = configOpt.get();
            List<ValidationResult> results = executeValidation(config);
            logger.info("Completed execution of validations for config ID: {}. Total: {}", configId, results.size());
            return results;
        } else {
            logger.warn("No comparison configuration found for ID: {}", configId);
            return new ArrayList<>();
        }
    }

    /**
     * Execute validation for a comparison configuration
     * @param config Comparison configuration
     * @return List of validation results
     */
    private List<ValidationResult> executeValidation(ComparisonConfig config) {
        List<ValidationResult> results = new ArrayList<>();

        if (!config.isEnabled()) {
            logger.info("Skipping validation for disabled config: {}", config.getId());
            return results;
        }

        // Execute day-over-day validations
        Optional<DayOverDayConfig> dayOverDayConfigOpt = dayOverDayConfigRepository.findByComparisonConfigId(config.getId());
        if (dayOverDayConfigOpt.isPresent()) {
            DayOverDayConfig dayOverDayConfig = dayOverDayConfigOpt.get();
            if (dayOverDayConfig.isEnabled()) {
                try {
                    ValidationResult result = thresholdValidator.validateDayOverDay(dayOverDayConfig);
                    results.add(result);
                } catch (Exception e) {
                    logger.error("Error executing day-over-day validation for config: " + config.getId(), e);
                    ValidationResult result = new ValidationResult();
                    result.setComparisonConfig(config);
                    result.setSuccess(false);
                    result.setErrorMessage("Error: " + e.getMessage());
                    result.setExecutionDate(LocalDateTime.now());
                    results.add(result);
                }
            }
        }

        // Execute cross-table validations
        List<CrossTableConfig> crossTableConfigs = crossTableConfigRepository.findBySourceComparisonConfigAndEnabled(config, true);
        for (CrossTableConfig crossTableConfig : crossTableConfigs) {
            try {
                ValidationResult result = thresholdValidator.validateCrossTable(crossTableConfig);
                results.add(result);
            } catch (Exception e) {
                logger.error("Error executing cross-table validation for config: " + config.getId() +
                        " and target table: " + crossTableConfig.getTargetTableName(), e);
                ValidationResult result = new ValidationResult();
                result.setComparisonConfig(config);
                result.setSuccess(false);
                result.setErrorMessage("Error: " + e.getMessage());
                result.setExecutionDate(LocalDateTime.now());
                results.add(result);
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
        logger.info("Retrying validation for result ID: {}", validationResultId);

        // Get the failed validation result
        Optional<ValidationResult> failedResultOpt = Optional.ofNullable(null); // Replace with actual repository call

        if (failedResultOpt.isPresent()) {
            ValidationResult failedResult = failedResultOpt.get();
            ComparisonConfig config = failedResult.getComparisonConfig();

            // Execute validation for this config
            List<ValidationResult> results = executeValidation(config);

            if (!results.isEmpty()) {
                return results.get(0);
            } else {
                logger.warn("No results from retry for validation result ID: {}", validationResultId);
                return null;
            }
        } else {
            logger.warn("No validation result found for ID: {}", validationResultId);
            return null;
        }
    }
}
