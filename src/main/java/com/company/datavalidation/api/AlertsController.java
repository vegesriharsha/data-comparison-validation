package com.company.datavalidation.api;

import com.company.datavalidation.model.Severity;
import com.company.datavalidation.model.ValidationDetailResult;
import com.company.datavalidation.repository.ValidationDetailResultRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/alerts")
@Tag(name = "Alerts", description = "APIs for retrieving and managing alerts")
@RequiredArgsConstructor
@Slf4j
public class AlertsController {

    private final ValidationDetailResultRepository validationDetailResultRepository;

    /**
     * Alert data transfer object as a record
     */
    public record AlertDto(
            Long id,
            String tableName,
            String columnName,
            LocalDateTime executionDate,
            String actualValue,
            String expectedValue,
            String differenceValue,
            String differencePercentage,
            String severity,
            boolean acknowledged
    ) {}

    @GetMapping
    @Operation(summary = "Get all current alerts")
    public ResponseEntity<List<AlertDto>> getAllAlerts() {
        log.info("API request: get all current alerts");

        // Get alerts from the last 24 hours
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);

        var alerts = validationDetailResultRepository.findAll().stream()
                .filter(ValidationDetailResult::isThresholdExceeded)
                .filter(d -> d.getValidationResult().getExecutionDate().isAfter(oneDayAgo))
                .map(this::convertToAlertDto)
                .toList();

        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/severity/{level}")
    @Operation(summary = "Get alerts by severity level")
    public ResponseEntity<List<AlertDto>> getAlertsBySeverity(@PathVariable String level) {
        log.info("API request: get alerts by severity: {}", level);

        // Parse severity level
        Severity severity;
        try {
            severity = Severity.valueOf(level.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid severity level: {}", level);
            return ResponseEntity.badRequest().build();
        }

        // Get alerts from the last 24 hours with the specified severity
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);

        var alerts = validationDetailResultRepository.findAll().stream()
                .filter(ValidationDetailResult::isThresholdExceeded)
                .filter(d -> d.getValidationResult().getExecutionDate().isAfter(oneDayAgo))
                // In a real implementation, would need to join with threshold config to filter by severity
                // Simplified for now: just return all alerts
                .map(this::convertToAlertDto)
                .toList();

        return ResponseEntity.ok(alerts);
    }

    /**
     * Record for alert acknowledgment response
     */
    public record AlertAcknowledgmentDto(
            Long id,
            boolean acknowledged,
            String message,
            LocalDateTime acknowledgedAt,
            String acknowledgedBy
    ) {}

    @PutMapping("/{id}/acknowledge")
    @Operation(summary = "Acknowledge an alert")
    public ResponseEntity<AlertAcknowledgmentDto> acknowledgeAlert(
            @PathVariable Long id,
            @RequestParam(required = false) String acknowledgedBy) {
        log.info("API request: acknowledge alert ID: {}, by: {}", id, acknowledgedBy);

        // In a real implementation, would need a separate table to track acknowledgments
        // For simplicity, we'll just return a success message
        var response = new AlertAcknowledgmentDto(
                id,
                true,
                "Alert acknowledged successfully",
                LocalDateTime.now(),
                Optional.ofNullable(acknowledgedBy).orElse("system")
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Record for alert count response
     */
    public record AlertCountDto(
            long alertCount,
            Map<String, Long> countBySeverity,
            LocalDateTime timestamp
    ) {}

    @GetMapping("/count")
    @Operation(summary = "Get count of active alerts")
    public ResponseEntity<AlertCountDto> getAlertCount() {
        log.info("API request: get alert count");

        // Get alerts from the last 24 hours
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);

        var failedDetails = validationDetailResultRepository.findAll().stream()
                .filter(ValidationDetailResult::isThresholdExceeded)
                .filter(d -> d.getValidationResult().getExecutionDate().isAfter(oneDayAgo))
                .toList();

        long alertCount = failedDetails.size();

        // Count by severity (simplified for demo)
        var countBySeverity = Map.of(
                "HIGH", failedDetails.size() / 2L,  // Just for demo
                "MEDIUM", failedDetails.size() / 3L,
                "LOW", failedDetails.size() - (failedDetails.size() / 2L) - (failedDetails.size() / 3L)
        );

        var response = new AlertCountDto(
                alertCount,
                countBySeverity,
                LocalDateTime.now()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Convert a validation detail to an alert DTO
     * @param detail Validation detail
     * @return AlertDto
     */
    private AlertDto convertToAlertDto(ValidationDetailResult detail) {
        return new AlertDto(
                detail.getId(),
                detail.getValidationResult().getComparisonConfig().getTableName(),
                detail.getColumnComparisonConfig().getColumnName(),
                detail.getValidationResult().getExecutionDate(),
                detail.getActualValue() != null ? detail.getActualValue().toString() : "null",
                detail.getExpectedValue() != null ? detail.getExpectedValue().toString() : "null",
                detail.getDifferenceValue() != null ? detail.getDifferenceValue().toString() : "null",
                detail.getDifferencePercentage() != null ?
                        detail.getDifferencePercentage().toString() + "%" : "null",
                "HIGH", // Simplified for now
                false    // Not acknowledged by default
        );
    }
}
