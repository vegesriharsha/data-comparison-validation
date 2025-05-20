package com.company.datavalidation.api;

import com.company.datavalidation.model.ValidationResult;
import com.company.datavalidation.repository.ValidationDetailResultRepository;
import com.company.datavalidation.repository.ValidationResultRepository;
import com.company.datavalidation.service.validation.ValidationExecutor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/executions")
@Tag(name = "Validation Execution", description = "APIs for executing and managing validations")
@RequiredArgsConstructor
@Slf4j
public class ExecutionController {

    private final ValidationExecutor validationExecutor;
    private final ValidationResultRepository validationResultRepository;
    private final ValidationDetailResultRepository validationDetailResultRepository;

    /**
     * Record for detailed validation results
     */
    public record ValidationDetailDto(
            Long id,
            String tableName,
            String columnName,
            String comparisonType,
            String actualValue,
            String expectedValue,
            String differenceValue,
            String differencePercentage,
            boolean thresholdExceeded
    ) {}

    @PostMapping
    @Operation(summary = "Execute all enabled validations")
    public ResponseEntity<List<ValidationResult>> executeAllValidations() {
        log.info("API request: execute all validations");
        List<ValidationResult> results = validationExecutor.executeAllValidations();
        return ResponseEntity.ok(results);
    }

    @PostMapping("/tables/{tableName}")
    @Operation(summary = "Execute validations for specific table")
    public ResponseEntity<List<ValidationResult>> executeValidationForTable(@PathVariable String tableName) {
        log.info("API request: execute validations for table: {}", tableName);
        List<ValidationResult> results = validationExecutor.executeValidationForTable(tableName);
        return ResponseEntity.ok(results);
    }

    @PostMapping("/configs/{configId}")
    @Operation(summary = "Execute specific configuration")
    public ResponseEntity<List<ValidationResult>> executeValidationForConfig(@PathVariable Long configId) {
        log.info("API request: execute validations for config: {}", configId);
        List<ValidationResult> results = validationExecutor.executeValidationForConfig(configId);
        return ResponseEntity.ok(results);
    }

    @GetMapping
    @Operation(summary = "List execution history")
    public ResponseEntity<Page<ValidationResult>> getExecutionHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("API request: get execution history, page: {}, size: {}", page, size);

        Page<ValidationResult> results = validationResultRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "executionDate")));

        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get execution details")
    public ResponseEntity<ValidationResult> getExecutionDetails(@PathVariable Long id) {
        log.info("API request: get execution details for ID: {}", id);

        return validationResultRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    log.warn("Validation result not found for ID: {}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @GetMapping("/{id}/results")
    @Operation(summary = "Get detailed validation results")
    public ResponseEntity<List<ValidationDetailDto>> getDetailedResults(@PathVariable Long id) {
        log.info("API request: get detailed results for validation ID: {}", id);

        var validationResult = validationResultRepository.findById(id)
                .orElse(null);

        if (validationResult == null) {
            log.warn("Validation result not found for ID: {}", id);
            return ResponseEntity.notFound().build();
        }

        var detailResults = validationDetailResultRepository.findByValidationResult(validationResult);

        // Map to DTOs
        var detailDtos = detailResults.stream()
                .map(detail -> new ValidationDetailDto(
                        detail.getId(),
                        detail.getValidationResult().getComparisonConfig().getTableName(),
                        detail.getColumnComparisonConfig().getColumnName(),
                        detail.getColumnComparisonConfig().getComparisonType().toString(),
                        detail.getActualValue() != null ? detail.getActualValue().toString() : "null",
                        detail.getExpectedValue() != null ? detail.getExpectedValue().toString() : "null",
                        detail.getDifferenceValue() != null ? detail.getDifferenceValue().toString() : "null",
                        detail.getDifferencePercentage() != null ?
                                detail.getDifferencePercentage().toString() + "%" : "null",
                        detail.isThresholdExceeded()
                ))
                .toList();

        return ResponseEntity.ok(detailDtos);
    }

    @PostMapping("/{id}/retry")
    @Operation(summary = "Retry a failed validation")
    public ResponseEntity<ValidationResult> retryValidation(@PathVariable Long id) {
        log.info("API request: retry validation for ID: {}", id);

        ValidationResult result = validationExecutor.retryValidation(id);

        return Optional.ofNullable(result)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    log.warn("Could not retry validation for ID: {}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Record for validation execution summary
     */
    public record ValidationSummaryDto(
            long totalExecutions,
            long successfulExecutions,
            long failedExecutions,
            double successRate,
            LocalDateTime lastExecutionTime
    ) {}

    @GetMapping("/summary")
    @Operation(summary = "Get validation execution summary")
    public ResponseEntity<ValidationSummaryDto> getExecutionSummary() {
        log.info("API request: get execution summary");

        LocalDateTime startDate = LocalDateTime.now().minusDays(7);

        long totalExecutions = validationResultRepository.count();
        long successfulExecutions = validationResultRepository.countBySuccessAndExecutionDateAfter(true, startDate);
        long failedExecutions = validationResultRepository.countBySuccessAndExecutionDateAfter(false, startDate);

        double successRate = totalExecutions > 0
                ? (double) successfulExecutions / totalExecutions * 100
                : 0;

        // Get last execution time
        LocalDateTime lastExecutionTime = validationResultRepository.findAll(
                        PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "executionDate")))
                .getContent()
                .stream()
                .findFirst()
                .map(ValidationResult::getExecutionDate)
                .orElse(null);

        var summary = new ValidationSummaryDto(
                totalExecutions,
                successfulExecutions,
                failedExecutions,
                successRate,
                lastExecutionTime
        );

        return ResponseEntity.ok(summary);
    }
}
