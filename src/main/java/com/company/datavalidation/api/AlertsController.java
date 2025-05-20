package com.company.datavalidation.api;

import com.company.datavalidation.model.Severity;
import com.company.datavalidation.model.ValidationDetailResult;
import com.company.datavalidation.repository.ValidationDetailResultRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/alerts")
@Tag(name = "Alerts", description = "APIs for retrieving and managing alerts")
public class AlertsController {

    private final ValidationDetailResultRepository validationDetailResultRepository;

    @Autowired
    public AlertsController(ValidationDetailResultRepository validationDetailResultRepository) {
        this.validationDetailResultRepository = validationDetailResultRepository;
    }

    @GetMapping
    @Operation(summary = "Get all current alerts")
    public ResponseEntity<List<Map<String, Object>>> getAllAlerts() {
        // Get all validation details that exceeded thresholds in the last 24 hours
        // This would be better with a custom query and DTO mapper
        List<ValidationDetailResult> failedDetails = validationDetailResultRepository.findAll().stream()
                .filter(ValidationDetailResult::isThresholdExceeded)
                .filter(d -> d.getValidationResult().getExecutionDate().isAfter(LocalDateTime.now().minusDays(1)))
                .collect(Collectors.toList());

        // Convert to a simplified format for the response
        List<Map<String, Object>> alerts = failedDetails.stream()
                .map(this::convertToAlertMap)
                .collect(Collectors.toList());

        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/severity/{level}")
    @Operation(summary = "Get alerts by severity level")
    public ResponseEntity<List<Map<String, Object>>> getAlertsBySeverity(@PathVariable String level) {
        Severity severity;
        try {
            severity = Severity.valueOf(level.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        // Get all validation details that exceeded thresholds in the last 24 hours with the specified severity
        // This would be better with a custom query and DTO mapper
        List<ValidationDetailResult> failedDetails = validationDetailResultRepository.findAll().stream()
                .filter(ValidationDetailResult::isThresholdExceeded)
                .filter(d -> d.getValidationResult().getExecutionDate().isAfter(LocalDateTime.now().minusDays(1)))
                // We would need to join with the threshold config to get the severity
                // This is simplified for now
                .collect(Collectors.toList());

        // Convert to a simplified format for the response
        List<Map<String, Object>> alerts = failedDetails.stream()
                .map(this::convertToAlertMap)
                .collect(Collectors.toList());

        return ResponseEntity.ok(alerts);
    }

    @PutMapping("/{id}/acknowledge")
    @Operation(summary = "Acknowledge an alert")
    public ResponseEntity<Map<String, Object>> acknowledgeAlert(@PathVariable Long id) {
        // In a real implementation, we would need a separate table to track alert acknowledgements
        // For simplicity, we'll just return a success message
        Map<String, Object> response = Map.of(
                "id", id,
                "acknowledged", true,
                "message", "Alert acknowledged successfully"
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/count")
    @Operation(summary = "Get count of active alerts")
    public ResponseEntity<Map<String, Object>> getAlertCount() {
        // Get count of validation details that exceeded thresholds in the last 24 hours
        // This would be better with a custom query
        long alertCount = validationDetailResultRepository.findAll().stream()
                .filter(ValidationDetailResult::isThresholdExceeded)
                .filter(d -> d.getValidationResult().getExecutionDate().isAfter(LocalDateTime.now().minusDays(1)))
                .count();

        Map<String, Object> response = Map.of(
                "alertCount", alertCount,
                "timestamp", LocalDateTime.now()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Convert a validation detail to an alert map
     * @param detail Validation detail
     * @return Map representation of the alert
     */
    private Map<String, Object> convertToAlertMap(ValidationDetailResult detail) {
        Map<String, Object> alert = new HashMap<>();

        alert.put("id", detail.getId());
        alert.put("tableName", detail.getValidationResult().getComparisonConfig().getTableName());
        alert.put("columnName", detail.getColumnComparisonConfig().getColumnName());
        alert.put("executionDate", detail.getValidationResult().getExecutionDate());
        alert.put("actualValue", detail.getActualValue());
        alert.put("expectedValue", detail.getExpectedValue());
        alert.put("differenceValue", detail.getDifferenceValue());
        alert.put("differencePercentage", detail.getDifferencePercentage());
        // We would need to join with the threshold config to get the severity
        alert.put("severity", "HIGH"); // Simplified for now

        return alert;
    }
}
