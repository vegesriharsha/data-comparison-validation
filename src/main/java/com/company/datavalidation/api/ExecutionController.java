package com.company.datavalidation.api;

import com.company.datavalidation.model.ValidationResult;
import com.company.datavalidation.repository.ValidationDetailResultRepository;
import com.company.datavalidation.repository.ValidationResultRepository;
import com.company.datavalidation.service.validation.ValidationExecutor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/executions")
@Tag(name = "Validation Execution", description = "APIs for executing and managing validations")
public class ExecutionController {

    private final ValidationExecutor validationExecutor;
    private final ValidationResultRepository validationResultRepository;
    private final ValidationDetailResultRepository validationDetailResultRepository;

    @Autowired
    public ExecutionController(
            ValidationExecutor validationExecutor,
            ValidationResultRepository validationResultRepository,
            ValidationDetailResultRepository validationDetailResultRepository) {
        this.validationExecutor = validationExecutor;
        this.validationResultRepository = validationResultRepository;
        this.validationDetailResultRepository = validationDetailResultRepository;
    }

    @PostMapping
    @Operation(summary = "Execute all enabled validations")
    public ResponseEntity<List<ValidationResult>> executeAllValidations() {
        List<ValidationResult> results = validationExecutor.executeAllValidations();
        return ResponseEntity.ok(results);
    }

    @PostMapping("/tables/{tableName}")
    @Operation(summary = "Execute validations for specific table")
    public ResponseEntity<List<ValidationResult>> executeValidationForTable(@PathVariable String tableName) {
        List<ValidationResult> results = validationExecutor.executeValidationForTable(tableName);
        return ResponseEntity.ok(results);
    }

    @PostMapping("/configs/{configId}")
    @Operation(summary = "Execute specific configuration")
    public ResponseEntity<List<ValidationResult>> executeValidationForConfig(@PathVariable Long configId) {
        List<ValidationResult> results = validationExecutor.executeValidationForConfig(configId);
        return ResponseEntity.ok(results);
    }

    @GetMapping
    @Operation(summary = "List execution history")
    public ResponseEntity<Page<ValidationResult>> getExecutionHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<ValidationResult> results = validationResultRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "executionDate")));

        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get execution details")
    public ResponseEntity<ValidationResult> getExecutionDetails(@PathVariable Long id) {
        Optional<ValidationResult> result = validationResultRepository.findById(id);
        return result.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/results")
    @Operation(summary = "Get detailed validation results")
    public ResponseEntity<List<Map<String, Object>>> getDetailedResults(@PathVariable Long id) {
        // This would need a custom DTO mapper or a native query to fetch the results with their related data
        // For simplicity, we'll return a placeholder here
        return ResponseEntity.ok(List.of(Map.of("message", "Detailed results would be returned here")));
    }

    @PostMapping("/{id}/retry")
    @Operation(summary = "Retry a failed validation")
    public ResponseEntity<ValidationResult> retryValidation(@PathVariable Long id) {
        ValidationResult result = validationExecutor.retryValidation(id);

        if (result != null) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
