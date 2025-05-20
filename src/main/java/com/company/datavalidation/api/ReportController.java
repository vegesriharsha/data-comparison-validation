package com.company.datavalidation.api;

import com.company.datavalidation.service.reporting.ReportGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports", description = "APIs for generating and retrieving reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportGenerator reportGenerator;

    @GetMapping("/daily")
    @Operation(summary = "Get daily validation summary")
    public ResponseEntity<Map<String, Object>> getDailySummary() {
        log.info("API request: get daily validation summary");
        Map<String, Object> report = reportGenerator.generateDailySummaryReport();
        return ResponseEntity.ok(report);
    }

    @GetMapping("/tables/{tableName}")
    @Operation(summary = "Get report for specific table")
    public ResponseEntity<Map<String, Object>> getTableReport(@PathVariable String tableName) {
        log.info("API request: get report for table: {}", tableName);
        Map<String, Object> report = reportGenerator.generateTableReport(tableName);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/trend")
    @Operation(summary = "Get trend report (success rate over time)")
    public ResponseEntity<Map<String, Object>> getTrendReport(@RequestParam(defaultValue = "30") int days) {
        log.info("API request: get trend report for {} days", days);
        Map<String, Object> report = reportGenerator.generateTrendReport(days);
        return ResponseEntity.ok(report);
    }

    /**
     * Record for export report request parameters
     */
    public record ExportParameters(
            String reportType,
            String tableName,
            int days,
            String format
    ) {}

    @GetMapping("/export")
    @Operation(summary = "Export reports (with format parameter)")
    public ResponseEntity<byte[]> exportReport(
            @RequestParam String reportType,
            @RequestParam(required = false) String tableName,
            @RequestParam(required = false, defaultValue = "30") int days,
            @RequestParam(defaultValue = "excel") String format) throws IOException {

        log.info("API request: export report, type: {}, table: {}, days: {}, format: {}",
                reportType, tableName, days, format);

        // Switch expression for content type
        String contentType = switch (format.toLowerCase()) {
            case "excel" -> "application/vnd.ms-excel";
            case "csv" -> "text/csv";
            case "pdf" -> "application/pdf";
            default -> "application/octet-stream";
        };

        // Validate format
        if (!"excel".equals(format)) {
            log.warn("Unsupported export format: {}", format);
            return ResponseEntity.badRequest().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDispositionFormData("attachment", reportType + "-report.xlsx");

        // Using Java 21's Map.of for concise parameter mapping
        Map<String, Object> parameters = Map.of(
                "tableName", tableName != null ? tableName : "",
                "days", days
        );

        byte[] reportData = reportGenerator.exportReportAsExcel(reportType, parameters);
        return ResponseEntity.ok().headers(headers).body(reportData);
    }

    /**
     * Record for dashboard statistics
     */
    public record DashboardStats(
            int validationCount,
            double successRate,
            int failureCount,
            Map<String, Integer> failuresByTable,
            Map<String, Double> trendData,
            LocalDate reportDate
    ) {}

    @GetMapping("/dashboard")
    @Operation(summary = "Get dashboard statistics")
    public ResponseEntity<DashboardStats> getDashboardStats() {
        log.info("API request: get dashboard statistics");

        // In a real implementation, this would compute actual statistics
        var dailyReport = reportGenerator.generateDailySummaryReport();
        var trendReport = reportGenerator.generateTrendReport(7);

        long totalValidations = (Long)dailyReport.get("totalValidations");
        long successfulValidations = (Long)dailyReport.get("successfulValidations");
        long failedValidations = (Long)dailyReport.get("failedValidations");
        double successRate = (Double)dailyReport.get("successRate");

        // Example data for demonstration
        var failuresByTable = Map.of(
                "orders", 5,
                "customers", 2,
                "products", 3
        );

        var trendData = Map.of(
                "Monday", 92.5,
                "Tuesday", 88.7,
                "Wednesday", 95.2,
                "Thursday", 97.8,
                "Friday", 94.1,
                "Saturday", 92.3,
                "Sunday", 90.6
        );

        var stats = new DashboardStats(
                (int)totalValidations,
                successRate,
                (int)failedValidations,
                failuresByTable,
                trendData,
                LocalDate.now()
        );

        return ResponseEntity.ok(stats);
    }
}
