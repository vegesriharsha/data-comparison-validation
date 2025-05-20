package com.company.datavalidation.service.reporting;

import com.company.datavalidation.model.*;
import com.company.datavalidation.repository.ValidationDetailResultRepository;
import com.company.datavalidation.repository.ValidationResultRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportGenerator {

    private final ValidationResultRepository validationResultRepository;
    private final ValidationDetailResultRepository validationDetailResultRepository;

    @Autowired
    public ReportGenerator(
            ValidationResultRepository validationResultRepository,
            ValidationDetailResultRepository validationDetailResultRepository) {
        this.validationResultRepository = validationResultRepository;
        this.validationDetailResultRepository = validationDetailResultRepository;
    }

    /**
     * Generate a daily validation summary report
     * @return Map containing report data
     */
    public Map<String, Object> generateDailySummaryReport() {
        Map<String, Object> report = new HashMap<>();

        // Get today's date
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay().minusNanos(1);

        // Get all validation results for today
        List<ValidationResult> results = validationResultRepository.findByExecutionDateBetween(startOfDay, endOfDay);

        // Calculate summary metrics
        long totalValidations = results.size();
        long successfulValidations = results.stream().filter(ValidationResult::isSuccess).count();
        long failedValidations = totalValidations - successfulValidations;
        double successRate = totalValidations > 0 ? (double) successfulValidations / totalValidations * 100 : 0;

        // Group by table
        Map<String, List<ValidationResult>> resultsByTable = results.stream()
                .collect(Collectors.groupingBy(r -> r.getComparisonConfig().getTableName()));

        // Create table summaries
        List<Map<String, Object>> tableSummaries = new ArrayList<>();
        for (Map.Entry<String, List<ValidationResult>> entry : resultsByTable.entrySet()) {
            String tableName = entry.getKey();
            List<ValidationResult> tableResults = entry.getValue();

            long tableTotal = tableResults.size();
            long tableSuccess = tableResults.stream().filter(ValidationResult::isSuccess).count();
            long tableFailed = tableTotal - tableSuccess;
            double tableSuccessRate = tableTotal > 0 ? (double) tableSuccess / tableTotal * 100 : 0;

            Map<String, Object> tableSummary = new HashMap<>();
            tableSummary.put("tableName", tableName);
            tableSummary.put("totalValidations", tableTotal);
            tableSummary.put("successfulValidations", tableSuccess);
            tableSummary.put("failedValidations", tableFailed);
            tableSummary.put("successRate", tableSuccessRate);

            tableSummaries.add(tableSummary);
        }

        // Find failed validations with details
        List<Map<String, Object>> failureDetails = new ArrayList<>();
        for (ValidationResult result : results) {
            if (!result.isSuccess()) {
                // Get detail results
                List<ValidationDetailResult> detailResults = validationDetailResultRepository.findByValidationResultIdAndThresholdExceeded(
                        result.getId(), true);

                for (ValidationDetailResult detail : detailResults) {
                    Map<String, Object> failureDetail = new HashMap<>();
                    failureDetail.put("tableName", result.getComparisonConfig().getTableName());
                    failureDetail.put("executionDate", result.getExecutionDate());
                    failureDetail.put("columnName", detail.getColumnComparisonConfig().getColumnName());
                    failureDetail.put("actualValue", detail.getActualValue());
                    failureDetail.put("expectedValue", detail.getExpectedValue());
                    failureDetail.put("differenceValue", detail.getDifferenceValue());
                    failureDetail.put("differencePercentage", detail.getDifferencePercentage());

                    failureDetails.add(failureDetail);
                }
            }
        }

        // Populate the report
        report.put("reportDate", today);
        report.put("totalValidations", totalValidations);
        report.put("successfulValidations", successfulValidations);
        report.put("failedValidations", failedValidations);
        report.put("successRate", successRate);
        report.put("tableSummaries", tableSummaries);
        report.put("failureDetails", failureDetails);

        return report;
    }

    /**
     * Generate a report for a specific table
     * @param tableName Table name
     * @return Map containing report data
     */
    public Map<String, Object> generateTableReport(String tableName) {
        Map<String, Object> report = new HashMap<>();

        // Get latest validation results for this table (up to 100)
        Pageable pageable = PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "executionDate"));
        Page<ValidationResult> resultsPage = validationResultRepository.findByTableName(tableName, pageable);
        List<ValidationResult> results = resultsPage.getContent();

        // Calculate summary metrics
        long totalValidations = results.size();
        long successfulValidations = results.stream().filter(ValidationResult::isSuccess).count();
        long failedValidations = totalValidations - successfulValidations;
        double successRate = totalValidations > 0 ? (double) successfulValidations / totalValidations * 100 : 0;

        // Calculate average execution time
        OptionalDouble avgExecutionTime = results.stream()
                .filter(r -> r.getExecutionTimeMs() != null)
                .mapToInt(ValidationResult::getExecutionTimeMs)
                .average();

        // Find all failed validations with details
        List<Map<String, Object>> validationDetails = new ArrayList<>();
        for (ValidationResult result : results) {
            List<ValidationDetailResult> detailResults = validationDetailResultRepository.findByValidationResult(result);

            for (ValidationDetailResult detail : detailResults) {
                Map<String, Object> validationDetail = new HashMap<>();
                validationDetail.put("executionDate", result.getExecutionDate());
                validationDetail.put("success", result.isSuccess());
                validationDetail.put("columnName", detail.getColumnComparisonConfig().getColumnName());
                validationDetail.put("comparisonType", detail.getColumnComparisonConfig().getComparisonType());
                validationDetail.put("actualValue", detail.getActualValue());
                validationDetail.put("expectedValue", detail.getExpectedValue());
                validationDetail.put("differenceValue", detail.getDifferenceValue());
                validationDetail.put("differencePercentage", detail.getDifferencePercentage());
                validationDetail.put("thresholdExceeded", detail.isThresholdExceeded());

                validationDetails.add(validationDetail);
            }
        }

        // Populate the report
        report.put("tableName", tableName);
        report.put("totalValidations", totalValidations);
        report.put("successfulValidations", successfulValidations);
        report.put("failedValidations", failedValidations);
        report.put("successRate", successRate);
        report.put("averageExecutionTimeMs", avgExecutionTime.orElse(0));
        report.put("validationDetails", validationDetails);

        return report;
    }

    /**
     * Generate a trend report
     * @param days Number of days to include
     * @return Map containing report data
     */
    public Map<String, Object> generateTrendReport(int days) {
        Map<String, Object> report = new HashMap<>();

        List<Map<String, Object>> trends = new ArrayList<>();

        // Get start and end dates
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(days - 1);

        // For each day in the range
        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.plusDays(1).atStartOfDay().minusNanos(1);

            // Get validation results for this day
            List<ValidationResult> results = validationResultRepository.findByExecutionDateBetween(startOfDay, endOfDay);

            // Calculate metrics
            long totalValidations = results.size();
            long successfulValidations = results.stream().filter(ValidationResult::isSuccess).count();
            long failedValidations = totalValidations - successfulValidations;
            double successRate = totalValidations > 0 ? (double) successfulValidations / totalValidations * 100 : 0;

            Map<String, Object> dayTrend = new HashMap<>();
            dayTrend.put("date", date);
            dayTrend.put("totalValidations", totalValidations);
            dayTrend.put("successfulValidations", successfulValidations);
            dayTrend.put("failedValidations", failedValidations);
            dayTrend.put("successRate", successRate);

            trends.add(dayTrend);
        }

        report.put("startDate", startDate);
        report.put("endDate", today);
        report.put("trends", trends);

        return report;
    }

    /**
     * Export a report as Excel
     * @param reportType Type of report ("daily", "table", "trend")
     * @param parameters Additional parameters for the report
     * @return Excel workbook as byte array
     */
    public byte[] exportReportAsExcel(String reportType, Map<String, Object> parameters) throws IOException {
        Workbook workbook = new XSSFWorkbook();

        if ("daily".equals(reportType)) {
            createDailyReportSheet(workbook);
        } else if ("table".equals(reportType)) {
            String tableName = (String) parameters.get("tableName");
            createTableReportSheet(workbook, tableName);
        } else if ("trend".equals(reportType)) {
            Integer days = (Integer) parameters.get("days");
            if (days == null) {
                days = 30; // Default to 30 days
            }
            createTrendReportSheet(workbook, days);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }

    /**
     * Create a daily report sheet
     * @param workbook Excel workbook
     */
    private void createDailyReportSheet(Workbook workbook) {
        Map<String, Object> report = generateDailySummaryReport();

        // Create a sheet for the summary
        Sheet summarySheet = workbook.createSheet("Summary");

        // Create header row
        Row headerRow = summarySheet.createRow(0);
        headerRow.createCell(0).setCellValue("Report Date");
        headerRow.createCell(1).setCellValue("Total Validations");
        headerRow.createCell(2).setCellValue("Successful");
        headerRow.createCell(3).setCellValue("Failed");
        headerRow.createCell(4).setCellValue("Success Rate (%)");

        // Create data row
        Row dataRow = summarySheet.createRow(1);
        dataRow.createCell(0).setCellValue(((LocalDate) report.get("reportDate")).toString());
        dataRow.createCell(1).setCellValue((Long) report.get("totalValidations"));
        dataRow.createCell(2).setCellValue((Long) report.get("successfulValidations"));
        dataRow.createCell(3).setCellValue((Long) report.get("failedValidations"));
        dataRow.createCell(4).setCellValue((Double) report.get("successRate"));

        // Create a sheet for table summaries
        Sheet tableSheet = workbook.createSheet("Table Summaries");

        // Create header row
        Row tableHeaderRow = tableSheet.createRow(0);
        tableHeaderRow.createCell(0).setCellValue("Table Name");
        tableHeaderRow.createCell(1).setCellValue("Total Validations");
        tableHeaderRow.createCell(2).setCellValue("Successful");
        tableHeaderRow.createCell(3).setCellValue("Failed");
        tableHeaderRow.createCell(4).setCellValue("Success Rate (%)");

        // Create data rows
        List<Map<String, Object>> tableSummaries = (List<Map<String, Object>>) report.get("tableSummaries");
        for (int i = 0; i < tableSummaries.size(); i++) {
            Map<String, Object> tableSummary = tableSummaries.get(i);
            Row tableDataRow = tableSheet.createRow(i + 1);
            tableDataRow.createCell(0).setCellValue((String) tableSummary.get("tableName"));
            tableDataRow.createCell(1).setCellValue((Long) tableSummary.get("totalValidations"));
            tableDataRow.createCell(2).setCellValue((Long) tableSummary.get("successfulValidations"));
            tableDataRow.createCell(3).setCellValue((Long) tableSummary.get("failedValidations"));
            tableDataRow.createCell(4).setCellValue((Double) tableSummary.get("successRate"));
        }

        // Create a sheet for failure details
        Sheet failuresSheet = workbook.createSheet("Failure Details");

        // Create header row
        Row failuresHeaderRow = failuresSheet.createRow(0);
        failuresHeaderRow.createCell(0).setCellValue("Table Name");
        failuresHeaderRow.createCell(1).setCellValue("Execution Date");
        failuresHeaderRow.createCell(2).setCellValue("Column Name");
        failuresHeaderRow.createCell(3).setCellValue("Actual Value");
        failuresHeaderRow.createCell(4).setCellValue("Expected Value");
        failuresHeaderRow.createCell(5).setCellValue("Difference");
        failuresHeaderRow.createCell(6).setCellValue("Difference (%)");

        // Create data rows
        List<Map<String, Object>> failureDetails = (List<Map<String, Object>>) report.get("failureDetails");
        for (int i = 0; i < failureDetails.size(); i++) {
            Map<String, Object> detail = failureDetails.get(i);
            Row failureDataRow = failuresSheet.createRow(i + 1);
            failureDataRow.createCell(0).setCellValue((String) detail.get("tableName"));

            LocalDateTime executionDate = (LocalDateTime) detail.get("executionDate");
            failureDataRow.createCell(1).setCellValue(
                    executionDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            failureDataRow.createCell(2).setCellValue((String) detail.get("columnName"));

            BigDecimal actualValue = (BigDecimal) detail.get("actualValue");
            if (actualValue != null) {
                failureDataRow.createCell(3).setCellValue(actualValue.doubleValue());
            }

            BigDecimal expectedValue = (BigDecimal) detail.get("expectedValue");
            if (expectedValue != null) {
                failureDataRow.createCell(4).setCellValue(expectedValue.doubleValue());
            }

            BigDecimal differenceValue = (BigDecimal) detail.get("differenceValue");
            if (differenceValue != null) {
                failureDataRow.createCell(5).setCellValue(differenceValue.doubleValue());
            }

            BigDecimal differencePercentage = (BigDecimal) detail.get("differencePercentage");
            if (differencePercentage != null) {
                failureDataRow.createCell(6).setCellValue(differencePercentage.doubleValue());
            }
        }

        // Auto-size columns for better readability
        for (int i = 0; i < 7; i++) {
            summarySheet.autoSizeColumn(i);
            tableSheet.autoSizeColumn(i);
            failuresSheet.autoSizeColumn(i);
        }
    }

    /**
     * Create a table report sheet
     * @param workbook Excel workbook
     * @param tableName Table name
     */
    private void createTableReportSheet(Workbook workbook, String tableName) {
        Map<String, Object> report = generateTableReport(tableName);

        // Create a sheet for the summary
        Sheet summarySheet = workbook.createSheet("Summary");

        // Create header row
        Row headerRow = summarySheet.createRow(0);
        headerRow.createCell(0).setCellValue("Table Name");
        headerRow.createCell(1).setCellValue("Total Validations");
        headerRow.createCell(2).setCellValue("Successful");
        headerRow.createCell(3).setCellValue("Failed");
        headerRow.createCell(4).setCellValue("Success Rate (%)");
        headerRow.createCell(5).setCellValue("Avg Execution Time (ms)");

        // Create data row
        Row dataRow = summarySheet.createRow(1);
        dataRow.createCell(0).setCellValue((String) report.get("tableName"));
        dataRow.createCell(1).setCellValue((Long) report.get("totalValidations"));
        dataRow.createCell(2).setCellValue((Long) report.get("successfulValidations"));
        dataRow.createCell(3).setCellValue((Long) report.get("failedValidations"));
        dataRow.createCell(4).setCellValue((Double) report.get("successRate"));
        dataRow.createCell(5).setCellValue((Double) report.get("averageExecutionTimeMs"));

        // Create a sheet for validation details
        Sheet detailsSheet = workbook.createSheet("Validation Details");

        // Create header row
        Row detailsHeaderRow = detailsSheet.createRow(0);
        detailsHeaderRow.createCell(0).setCellValue("Execution Date");
        detailsHeaderRow.createCell(1).setCellValue("Success");
        detailsHeaderRow.createCell(2).setCellValue("Column Name");
        detailsHeaderRow.createCell(3).setCellValue("Comparison Type");
        detailsHeaderRow.createCell(4).setCellValue("Actual Value");
        detailsHeaderRow.createCell(5).setCellValue("Expected Value");
        detailsHeaderRow.createCell(6).setCellValue("Difference");
        detailsHeaderRow.createCell(7).setCellValue("Difference (%)");
        detailsHeaderRow.createCell(8).setCellValue("Threshold Exceeded");

        // Create data rows
        List<Map<String, Object>> validationDetails = (List<Map<String, Object>>) report.get("validationDetails");
        for (int i = 0; i < validationDetails.size(); i++) {
            Map<String, Object> detail = validationDetails.get(i);
            Row detailDataRow = detailsSheet.createRow(i + 1);

            LocalDateTime executionDate = (LocalDateTime) detail.get("executionDate");
            detailDataRow.createCell(0).setCellValue(
                    executionDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            detailDataRow.createCell(1).setCellValue((Boolean) detail.get("success"));
            detailDataRow.createCell(2).setCellValue((String) detail.get("columnName"));
            detailDataRow.createCell(3).setCellValue(detail.get("comparisonType").toString());

            BigDecimal actualValue = (BigDecimal) detail.get("actualValue");
            if (actualValue != null) {
                detailDataRow.createCell(4).setCellValue(actualValue.doubleValue());
            }

            BigDecimal expectedValue = (BigDecimal) detail.get("expectedValue");
            if (expectedValue != null) {
                detailDataRow.createCell(5).setCellValue(expectedValue.doubleValue());
            }

            BigDecimal differenceValue = (BigDecimal) detail.get("differenceValue");
            if (differenceValue != null) {
                detailDataRow.createCell(6).setCellValue(differenceValue.doubleValue());
            }

            BigDecimal differencePercentage = (BigDecimal) detail.get("differencePercentage");
            if (differencePercentage != null) {
                detailDataRow.createCell(7).setCellValue(differencePercentage.doubleValue());
            }

            detailDataRow.createCell(8).setCellValue((Boolean) detail.get("thresholdExceeded"));
        }

        // Auto-size columns for better readability
        for (int i = 0; i < 9; i++) {
            summarySheet.autoSizeColumn(i);
            detailsSheet.autoSizeColumn(i);
        }
    }

    /**
     * Create a trend report sheet
     * @param workbook Excel workbook
     * @param days Number of days to include
     */
    private void createTrendReportSheet(Workbook workbook, int days) {
        Map<String, Object> report = generateTrendReport(days);

        // Create a sheet for the trend data
        Sheet trendSheet = workbook.createSheet("Trend Data");

        // Create header row
        Row headerRow = trendSheet.createRow(0);
        headerRow.createCell(0).setCellValue("Date");
        headerRow.createCell(1).setCellValue("Total Validations");
        headerRow.createCell(2).setCellValue("Successful");
        headerRow.createCell(3).setCellValue("Failed");
        headerRow.createCell(4).setCellValue("Success Rate (%)");

        // Create data rows
        List<Map<String, Object>> trends = (List<Map<String, Object>>) report.get("trends");
        for (int i = 0; i < trends.size(); i++) {
            Map<String, Object> trend = trends.get(i);
            Row dataRow = trendSheet.createRow(i + 1);

            LocalDate date = (LocalDate) trend.get("date");
            dataRow.createCell(0).setCellValue(date.toString());

            dataRow.createCell(1).setCellValue((Long) trend.get("totalValidations"));
            dataRow.createCell(2).setCellValue((Long) trend.get("successfulValidations"));
            dataRow.createCell(3).setCellValue((Long) trend.get("failedValidations"));
            dataRow.createCell(4).setCellValue((Double) trend.get("successRate"));
        }

        // Auto-size columns for better readability
        for (int i = 0; i < 5; i++) {
            trendSheet.autoSizeColumn(i);
        }
    }
}
