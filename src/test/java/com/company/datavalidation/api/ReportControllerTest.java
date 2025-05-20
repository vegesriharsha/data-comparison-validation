package com.company.datavalidation.api;

import com.company.datavalidation.service.reporting.ReportGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
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
public class ReportControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ReportGenerator reportGenerator;

    @InjectMocks
    private ReportController reportController;

    private Map<String, Object> dailyReport;
    private Map<String, Object> tableReport;
    private Map<String, Object> trendReport;
    private byte[] excelData;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(reportController).build();

        // Setup test data
        dailyReport = new HashMap<>();
        dailyReport.put("reportDate", LocalDate.now());
        dailyReport.put("totalValidations", 100L);
        dailyReport.put("successfulValidations", 90L);
        dailyReport.put("failedValidations", 10L);
        dailyReport.put("successRate", 90.0);

        List<Map<String, Object>> tableSummaries = new ArrayList<>();
        Map<String, Object> tableSummary = new HashMap<>();
        tableSummary.put("tableName", "orders");
        tableSummary.put("totalValidations", 50L);
        tableSummary.put("successfulValidations", 45L);
        tableSummary.put("failedValidations", 5L);
        tableSummary.put("successRate", 90.0);
        tableSummaries.add(tableSummary);
        dailyReport.put("tableSummaries", tableSummaries);

        List<Map<String, Object>> failureDetails = new ArrayList<>();
        Map<String, Object> failureDetail = new HashMap<>();
        failureDetail.put("tableName", "orders");
        failureDetail.put("columnName", "total_amount");
        failureDetail.put("differencePercentage", 15.0);
        failureDetails.add(failureDetail);
        dailyReport.put("failureDetails", failureDetails);

        // Table report
        tableReport = new HashMap<>();
        tableReport.put("tableName", "orders");
        tableReport.put("totalValidations", 50L);
        tableReport.put("successfulValidations", 45L);
        tableReport.put("failedValidations", 5L);
        tableReport.put("successRate", 90.0);
        tableReport.put("averageExecutionTimeMs", 175.0);

        List<Map<String, Object>> validationDetails = new ArrayList<>();
        Map<String, Object> validationDetail = new HashMap<>();
        validationDetail.put("columnName", "total_amount");
        validationDetail.put("comparisonType", "PERCENTAGE");
        validationDetail.put("thresholdExceeded", true);
        validationDetails.add(validationDetail);
        tableReport.put("validationDetails", validationDetails);

        // Trend report
        trendReport = new HashMap<>();
        trendReport.put("startDate", LocalDate.now().minusDays(29));
        trendReport.put("endDate", LocalDate.now());

        List<Map<String, Object>> trends = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            Map<String, Object> dayTrend = new HashMap<>();
            dayTrend.put("date", LocalDate.now().minusDays(i));
            dayTrend.put("totalValidations", 50L);
            dayTrend.put("successfulValidations", 45L);
            dayTrend.put("failedValidations", 5L);
            dayTrend.put("successRate", 90.0);
            trends.add(dayTrend);
        }
        trendReport.put("trends", trends);

        // Excel data
        excelData = "Excel data".getBytes();
    }

    @Test
    public void testGetDailySummary() throws Exception {
        when(reportGenerator.generateDailySummaryReport()).thenReturn(dailyReport);

        mockMvc.perform(get("/api/v1/reports/daily"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportDate", notNullValue()))
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
    public void testGetTableReport() throws Exception {
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
    public void testGetTrendReport() throws Exception {
        when(reportGenerator.generateTrendReport(30)).thenReturn(trendReport);

        mockMvc.perform(get("/api/v1/reports/trend")
                        .param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startDate", notNullValue()))
                .andExpect(jsonPath("$.endDate", notNullValue()))
                .andExpect(jsonPath("$.trends", hasSize(30)));

        verify(reportGenerator).generateTrendReport(30);
    }

    @Test
    public void testExportReport() throws Exception {
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
    public void testExportReport_TableSpecific() throws Exception {
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
    public void testExportReport_InvalidFormat() throws Exception {
        mockMvc.perform(get("/api/v1/reports/export")
                        .param("reportType", "daily")
                        .param("format", "invalid"))
                .andExpect(status().isBadRequest());
    }
}
