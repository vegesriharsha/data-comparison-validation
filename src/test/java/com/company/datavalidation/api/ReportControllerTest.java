package com.company.datavalidation.api;

import com.company.datavalidation.service.reporting.ReportGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Report Controller Tests")
class ReportControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ReportGenerator reportGenerator;

    @InjectMocks
    private ReportController reportController;

    // Test data
    private Map<String, Object> dailyReport;
    private Map<String, Object> tableReport;
    private Map<String, Object> trendReport;
    private byte[] excelData;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(reportController).build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // For handling Java 8 date/time

        // Setup test data
        // Using Java 21 Map.of for concise immutable maps

        // Example data for table summaries
        var tableSummary = Map.of(
                "tableName", "orders",
                "totalValidations", 50L,
                "successfulValidations", 45L,
                "failedValidations", 5L,
                "successRate", 90.0
        );

        // Example data for failure details
        var failureDetail = Map.of(
                "tableName", "orders",
                "columnName", "total_amount",
                "differencePercentage", 15.0
        );

        // Daily report
        dailyReport = Map.of(
                "reportDate", LocalDate.now(),
                "totalValidations", 100L,
                "successfulValidations", 90L,
                "failedValidations", 10L,
                "successRate", 90.0,
                "tableSummaries", List.of(tableSummary),
                "failureDetails", List.of(failureDetail)
        );

        // Table report
        var validationDetail = Map.of(
                "columnName", "total_amount",
                "comparisonType", "PERCENTAGE",
                "thresholdExceeded", true
        );

        tableReport = Map.of(
                "tableName", "orders",
                "totalValidations", 50L,
                "successfulValidations", 45L,
                "failedValidations", 5L,
                "successRate", 90.0,
                "averageExecutionTimeMs", 175.0,
                "validationDetails", List.of(validationDetail)
        );

        // Trend report
        var trends = List.of(
                Map.of(
                        "date", LocalDate.now().minusDays(0),
                        "totalValidations", 50L,
                        "successfulValidations", 45L,
                        "failedValidations", 5L,
                        "successRate", 90.0
                ),
                Map.of(
                        "date", LocalDate.now().minusDays(1),
                        "totalValidations",
                        48L,
                        "successfulValidations", 44L,
                        "failedValidations", 4L,
                        "successRate", 91.7
                )
        );

        trendReport = Map.of(
                "startDate", LocalDate.now().minusDays(29),
                "endDate", LocalDate.now(),
                "trends", trends
        );

        // Excel data
        excelData = "Excel data".getBytes();
    }

    @Test
    @DisplayName("Should get daily summary report")
    void testGetDailySummary() throws Exception {
        when(reportGenerator.generateDailySummaryReport()).thenReturn(dailyReport);

        mockMvc.perform(get("/api/v1/reports/daily"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportDate").exists())
                .andExpect(jsonPath("$.totalValidations", is(100)))
                .andExpect(jsonPath("$.successfulValidations", is(90)))
                .andExpect(jsonPath("$.failedValidations", is(10)))
                .andExpect(jsonPath("$.successRate", is(90.0)))
                .andExpect(jsonPath("$.tableSummaries", hasSize(1)))
                .andExpect(jsonPath("$.tableSummaries[0].tableName", is("orders")))
                .andExpect(jsonPath("$.failureDetails", hasSize(1)))
                .andExpect(jsonPath("$.failureDetails[0].tableName", is("orders")));

        verify(reportGenerator).generateDailySummaryReport();
    }

    @Test
    @DisplayName("Should get table report")
    void testGetTableReport() throws Exception {
        when(reportGenerator.generateTableReport("orders")).thenReturn(tableReport);

        mockMvc.perform(get("/api/v1/reports/tables/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tableName", is("orders")))
                .andExpect(jsonPath("$.totalValidations", is(50)))
                .andExpect(jsonPath("$.successfulValidations", is(45)))
                .andExpect(jsonPath("$.failedValidations", is(5)))
                .andExpect(jsonPath("$.successRate", is(90.0)))
                .andExpect(jsonPath("$.averageExecutionTimeMs", is(175.0)))
                .andExpect(jsonPath("$.validationDetails", hasSize(1)))
                .andExpect(jsonPath("$.validationDetails[0].columnName", is("total_amount")));

        verify(reportGenerator).generateTableReport("orders");
    }

    @Test
    @DisplayName("Should get trend report")
    void testGetTrendReport() throws Exception {
        when(reportGenerator.generateTrendReport(30)).thenReturn(trendReport);

        mockMvc.perform(get("/api/v1/reports/trend")
                        .param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startDate").exists())
                .andExpect(jsonPath("$.endDate").exists())
                .andExpect(jsonPath("$.trends").isArray());

        verify(reportGenerator).generateTrendReport(30);
    }

    @Test
    @DisplayName("Should export report as Excel")
    void testExportReport() throws Exception {
        when(reportGenerator.exportReportAsExcel(eq("daily"), any(Map.class))).thenReturn(excelData);

        mockMvc.perform(get("/api/v1/reports/export")
                        .param("reportType", "daily")
                        .param("format", "excel"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/vnd.ms-excel"))
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(content().bytes(excelData));

        verify(reportGenerator).exportReportAsExcel(eq("daily"), any(Map.class));
    }

    @Test
    @DisplayName("Should export report for specific table")
    void testExportReportTableSpecific() throws Exception {
        when(reportGenerator.exportReportAsExcel(eq("table"), any(Map.class))).thenReturn(excelData);

        mockMvc.perform(get("/api/v1/reports/export")
                        .param("reportType", "table")
                        .param("tableName", "orders")
                        .param("format", "excel"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/vnd.ms-excel"))
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(content().bytes(excelData));

        verify(reportGenerator).exportReportAsExcel(eq("table"), any(Map.class));
    }

    @Test
    @DisplayName("Should return bad request for invalid format")
    void testExportReportInvalidFormat() throws Exception {
        mockMvc.perform(get("/api/v1/reports/export")
                        .param("reportType", "daily")
                        .param("format", "invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should get dashboard statistics")
    void testGetDashboardStats() throws Exception {
        // Mock the daily and trend reports
        when(reportGenerator.generateDailySummaryReport()).thenReturn(dailyReport);
        when(reportGenerator.generateTrendReport(7)).thenReturn(trendReport);

        mockMvc.perform(get("/api/v1/reports/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validationCount").isNumber())
                .andExpect(jsonPath("$.successRate").isNumber())
                .andExpect(jsonPath("$.failureCount").isNumber())
                .andExpect(jsonPath("$.failuresByTable").exists())
                .andExpect(jsonPath("$.trendData").exists())
                .andExpect(jsonPath("$.reportDate").exists());
    }
}
