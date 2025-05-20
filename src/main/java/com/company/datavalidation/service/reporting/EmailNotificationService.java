package com.company.datavalidation.service.reporting;

import com.company.datavalidation.model.*;
import com.company.datavalidation.repository.EmailNotificationConfigRepository;
import com.company.datavalidation.repository.ValidationDetailResultRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;


import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EmailNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationService.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final EmailNotificationConfigRepository emailNotificationConfigRepository;
    private final ValidationDetailResultRepository validationDetailResultRepository;

    @Autowired
    public EmailNotificationService(
            JavaMailSender mailSender,
            TemplateEngine templateEngine,
            EmailNotificationConfigRepository emailNotificationConfigRepository,
            ValidationDetailResultRepository validationDetailResultRepository) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.emailNotificationConfigRepository = emailNotificationConfigRepository;
        this.validationDetailResultRepository = validationDetailResultRepository;
    }

    /**
     * Send a notification for a validation result
     * @param validationResult Validation result
     */
    public void sendNotification(ValidationResult validationResult) {
        if (validationResult.isSuccess()) {
            // Don't notify for successful validations
            return;
        }

        try {
            // Get validation details that exceeded thresholds
            List<ValidationDetailResult> failedDetails = validationDetailResultRepository.findByValidationResultIdAndThresholdExceeded(
                    validationResult.getId(), true);

            if (failedDetails.isEmpty()) {
                // No specific failures to report
                return;
            }

            // Get the highest severity among the failures
            Severity highestSeverity = getHighestSeverity(failedDetails);

            // Get email recipients for this severity
            List<EmailNotificationConfig> emailConfigs = emailNotificationConfigRepository.findBySeverityLevelAndEnabled(
                    highestSeverity, true);

            if (emailConfigs.isEmpty()) {
                logger.info("No email recipients configured for severity: {}", highestSeverity);
                return;
            }

            // Send notifications
            for (EmailNotificationConfig emailConfig : emailConfigs) {
                sendValidationFailureEmail(emailConfig.getEmailAddress(), validationResult, failedDetails, highestSeverity);
            }

        } catch (Exception e) {
            logger.error("Error sending notification for validation result: " + validationResult.getId(), e);
        }
    }

    /**
     * Get the highest severity from a list of validation details
     * @param details List of validation details
     * @return Highest severity
     */
    private Severity getHighestSeverity(List<ValidationDetailResult> details) {
        // Default to LOW if no specific severity is found
        Severity highestSeverity = Severity.LOW;

        for (ValidationDetailResult detail : details) {
            ColumnComparisonConfig columnConfig = detail.getColumnComparisonConfig();
            // Get threshold config - this is simplified, would need proper repository call
            // ThresholdConfig thresholdConfig = thresholdConfigRepository.findByColumnComparisonConfigIdAndSeverity(
            //         columnConfig.getId(), anyOf(Severity.values()));

            // For now, just use HIGH as the highest severity
            return Severity.HIGH;
        }

        return highestSeverity;
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

        // Prepare the email content
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

        logger.info("Sent validation failure notification to: {}", recipient);
    }

    /**
     * Convert a validation detail to a map for template rendering
     * @param detail Validation detail
     * @return Map representation of the detail
     */
    private Map<String, Object> convertDetailToMap(ValidationDetailResult detail) {
        Map<String, Object> map = new HashMap<>();

        map.put("columnName", detail.getColumnComparisonConfig().getColumnName());
        map.put("comparisonType", detail.getColumnComparisonConfig().getComparisonType());

        BigDecimal actualValue = detail.getActualValue();
        BigDecimal expectedValue = detail.getExpectedValue();
        BigDecimal differenceValue = detail.getDifferenceValue();
        BigDecimal differencePercentage = detail.getDifferencePercentage();

        map.put("actualValue", actualValue != null ? actualValue.toString() : "null");
        map.put("expectedValue", expectedValue != null ? expectedValue.toString() : "null");
        map.put("differenceValue", differenceValue != null ? differenceValue.toString() : "null");
        map.put("differencePercentage", differencePercentage != null ?
                differencePercentage.toString() + "%" : "null");

        return map;
    }

    /**
     * Send a daily summary report
     */
    public void sendDailySummaryReport() {
        try {
            // Get all email recipients
            List<EmailNotificationConfig> emailConfigs = emailNotificationConfigRepository.findByEnabled(true);

            if (emailConfigs.isEmpty()) {
                logger.info("No email recipients configured for daily summary report");
                return;
            }

            // Generate the report
            ReportGenerator reportGenerator = new ReportGenerator(null, null); // Inject proper dependencies
            Map<String, Object> report = reportGenerator.generateDailySummaryReport();

            // Send the report to each recipient
            for (EmailNotificationConfig emailConfig : emailConfigs) {
                sendDailySummaryEmail(emailConfig.getEmailAddress(), report);
            }

        } catch (Exception e) {
            logger.error("Error sending daily summary report", e);
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

        // Prepare the email content
        Context context = new Context();
        context.setVariable("reportDate", report.get("reportDate"));
        context.setVariable("totalValidations", report.get("totalValidations"));
        context.setVariable("successfulValidations", report.get("successfulValidations"));
        context.setVariable("failedValidations", report.get("failedValidations"));
        context.setVariable("successRate", report.get("successRate"));
        context.setVariable("tableSummaries", report.get("tableSummaries"));
        context.setVariable("failureDetails", report.get("failureDetails"));

        // Process the template
        String emailContent = templateEngine.process("daily-summary-email", context);
        helper.setText(emailContent, true);

        // Send the email
        mailSender.send(message);

        logger.info("Sent daily summary report to: {}", recipient);
    }
}
