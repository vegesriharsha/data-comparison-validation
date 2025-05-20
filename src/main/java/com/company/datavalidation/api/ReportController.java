package com.company.datavalidation.api;

import com.company.datavalidation.service.reporting.ReportGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports", description = "APIs for generating and retrieving reports")
public class ReportController {

    private final ReportGenerator reportGenerator;

    @Autowired
    public ReportController(ReportGenerator reportGenerator) {
        this.reportGenerator = reportGenerator;
    }

    @GetMapping("/daily")
    @Operation(summary = "Get daily validation summary")
    public ResponseEntity<Map<String, Object>> getDailySummary() {
        Map<String, Object> report = reportGenerator.generateDailySummaryReport();
        return ResponseEntity.ok(report);
    }

    @GetMapping("/tables/{tableName}")
    @Operation(summary = "Get report for specific table")
    public ResponseEntity<Map<String, Object>> getTableReport(@PathVariable String tableName) {
        Map<String, Object> report = reportGenerator.generateTableReport(tableName);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/trend")
    @Operation(summary = "Get trend report (success rate over time)")
    public ResponseEntity<Map<String, Object>> getTrendReport(@RequestParam(defaultValue = "30") int days) {
        Map<String, Object> report = reportGenerator.generateTrendReport(days);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/export")
    @Operation(summary = "Export reports (with format parameter)")
    public ResponseEntity<byte[]> exportReport(
            @RequestParam String reportType,
            @RequestParam(required = false) String tableName,
            @RequestParam(required = false, defaultValue = "30") int days,
            @RequestParam(defaultValue = "excel") String format) throws IOException {

        HttpHeaders headers = new HttpHeaders();

        if ("excel".equals(format)) {
            headers.setContentType(MediaType.parseMediaType("application/vnd.ms-excel"));
            headers.setContentDispositionFormData("attachment", reportType + "-report.xlsx");

            Map<String, Object> parameters = Map.of(
                    "tableName", tableName != null ? tableName : "",
                    "days", days
            );

            byte[] excelData = reportGenerator.exportReportAsExcel(reportType, parameters);
            return ResponseEntity.ok().headers(headers).body(excelData);
        } else {
            return ResponseEntity.badRequest().build();
        }
    }
}
