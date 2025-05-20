package com.company.datavalidation.service.reporting;

import com.company.datavalidation.model.*;
import com.company.datavalidation.repository.EmailNotificationConfigRepository;
import com.company.datavalidation.repository.ValidationDetailResultRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final EmailNotificationConfigRepository emailNotificationConfigRepository;
    private final ValidationDetailResultRepository validationDetailResultRepository;

    /**
     * Send a notification for a validation result
     * @param validationResult Validation result
     */
    public void sendNotification(ValidationResult validationResult) {
        if (validationResult.isSuccess()) {
            // Don't notify for successful validations
            log.debug("Skipping notification for successful validation: {}", validationResult.getId());
            return;
        }

        try {
            // Get validation details that exceeded thresholds
            List<ValidationDetailResult> failedDetails = validationDetailResultRepository
                    .findByValidationResultIdAndThresholdExceeded(
                            validationResult.getId(), true);

            if (failedDetails.isEmpty()) {
                // No specific failures to report
                log.debug("No threshold exceeded details found for validation: {}", validationResult.getId());
                return;
            }

            // Get the highest severity among the failures
            Severity highestSeverity = getHighestSeverity(failedDetails);
            log.debug("Highest severity for validation {}: {}", validationResult.getId(), highestSeverity);

            // Get email recipients for this severity
            List<EmailNotificationConfig> emailConfigs = emailNotificationConfigRepository
                    .findBySeverityLevelAndEnabled(highestSeverity, true);

            if (emailConfigs.isEmpty()) {
                log.info("No email recipients configured for severity: {}", highestSeverity);
                return;
            }

            // Send notifications using virtual threads
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                for (EmailNotificationConfig emailConfig : emailConfigs) {
                    executor.submit(() -> {
                        try {
                            sendValidationFailureEmail(
                                    emailConfig.getEmailAddress(),
                                    validationResult,
                                    failedDetails,
                                    highestSeverity
                            );
                        } catch (Exception e) {
                            log.error("Error sending notification to {}", emailConfig.getEmailAddress(), e);
                        }
                    });
                }
            }

        } catch (Exception e) {
            log.error("Error preparing notifications for validation result: {}", validationResult.getId(), e);
        }
    }

    /**
     * Get the highest severity from a list of validation details
     * @param details List of validation details
     * @return Highest severity
     */
    private Severity getHighestSeverity(List<ValidationDetailResult> details) {
        return details.stream()
                .map(detail -> {
                    // In a real implementation, would fetch the threshold config to get the severity
                    // For now, just use HIGH as a default (simplified implementation)
                    return Severity.HIGH;
                })
                .reduce(Severity.LOW, Severity::getHighest);
    }

    /**
     * Send an email notification for a validation failure
     * @param recipient Email recipient
     * @param validationResult Validation result
     * @param failedDetails List of failed validation details
     * @param severity Severity of the failure
     * @throws MessagingException if mail sending fails
     */
    private void sendValidationFailureEmail(String recipient, ValidationResult validationResult,
                                            List<ValidationDetailResult> failedDetails, Severity severity)
            throws MessagingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        // Set email properties
        helper.setTo(recipient);
        helper.setSubject(String.format("[%s] Data Validation Failure: %s",
                severity, validationResult.getComparisonConfig().getTableName()));

        // Prepare the email content using context
        Context context = new Context();
        context.setVariable("tableName", validationResult.getComparisonConfig().getTableName());
        context.setVariable("executionDate", validationResult.getExecutionDate().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        context.setVariable("severity", severity);

        // Convert failed details to a format suitable for the template
        List<Map<String, Object>> failureDetails = failedDetails.stream()
                .map(this::convertDetailToMap)
                .collect(Collectors.toList());

        context.setVariable("failureDetails", failureDetails);

        // Process the template
        String emailContent = templateEngine.process("validation-failure-email", context);
        helper.setText(emailContent, true);

        // Send the email
        mailSender.send(message);

        log.info("Sent validation failure notification to: {}", recipient);
    }

    /**
     * Convert a validation detail to a map for template rendering
     * @param detail Validation detail
     * @return Map representation of the detail
     */
    private Map<String, Object> convertDetailToMap(ValidationDetailResult detail) {
        return Map.of(
                "columnName", detail.getColumnComparisonConfig().getColumnName(),
                "comparisonType", detail.getColumnComparisonConfig().getComparisonType(),
                "actualValue", detail.getActualValue() != null ? detail.getActualValue().toString() : "null",
                "expectedValue", detail.getExpectedValue() != null ? detail.getExpectedValue().toString() : "null",
                "differenceValue", detail.getDifferenceValue() != null ? detail.getDifferenceValue().toString() : "null",
                "differencePercentage", detail.getDifferencePercentage() != null ?
                        detail.getDifferencePercentage().toString() + "%" : "null"
        );
    }

    /**
     * Send a daily summary report
     */
    public void sendDailySummaryReport(ReportGenerator reportGenerator) {
        try {
            // Get all email recipients
            List<EmailNotificationConfig> emailConfigs = emailNotificationConfigRepository.findByEnabled(true);

            if (emailConfigs.isEmpty()) {
                log.info("No email recipients configured for daily summary report");
                return;
            }

            // Generate the report
            Map<String, Object> report = reportGenerator.generateDailySummaryReport();
            log.debug("Generated daily summary report with {} table summaries",
                    ((List<?>) report.get("tableSummaries")).size());

            // Send the report to each recipient using virtual threads
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                for (EmailNotificationConfig emailConfig : emailConfigs) {
                    executor.submit(() -> {
                        try {
                            sendDailySummaryEmail(emailConfig.getEmailAddress(), report);
                        } catch (Exception e) {
                            log.error("Error sending daily summary to {}", emailConfig.getEmailAddress(), e);
                        }
                    });
                }
            }

        } catch (Exception e) {
            log.error("Error preparing daily summary report", e);
        }
    }

    /**
     * Send a daily summary email
     * @param recipient Email recipient
     * @param report Report data
     * @throws MessagingException if mail sending fails
     */
    private void sendDailySummaryEmail(String recipient, Map<String, Object> report) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        // Set email properties
        helper.setTo(recipient);
        helper.setSubject("Daily Data Validation Summary Report");

        // Prepare the email content using context
        Context context = new Context();
        context.setVariables(report);

        // Process the template
        String emailContent = templateEngine.process("daily-summary-email", context);
        helper.setText(emailContent, true);

        // Send the email
        mailSender.send(message);

        log.info("Sent daily summary report to: {}", recipient);
    }
}
