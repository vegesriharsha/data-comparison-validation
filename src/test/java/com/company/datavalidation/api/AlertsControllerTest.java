package com.company.datavalidation.api;

import com.company.datavalidation.model.*;
import com.company.datavalidation.repository.ValidationDetailResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class AlertsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ValidationDetailResultRepository validationDetailResultRepository;

    @InjectMocks
    private AlertsController alertsController;

    private List<ValidationDetailResult> validationDetailResults;
    private ComparisonConfig comparisonConfig;
    private ValidationResult validationResult;
    private ColumnComparisonConfig columnComparisonConfig;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(alertsController).build();

        // Setup test data
        comparisonConfig = new ComparisonConfig();
        comparisonConfig.setId(1L);
        comparisonConfig.setTableName("orders");
        comparisonConfig.setEnabled(true);

        validationResult = new ValidationResult();
        validationResult.setId(1L);
        validationResult.setComparisonConfig(comparisonConfig);
        validationResult.setExecutionDate(LocalDateTime.now().minusHours(1));
        validationResult.setSuccess(false);
        validationResult.setErrorMessage("Validation failed: threshold exceeded");

        DayOverDayConfig dayOverDayConfig = new DayOverDayConfig();
        dayOverDayConfig.setId(1L);
        dayOverDayConfig.setComparisonConfig(comparisonConfig);
        dayOverDayConfig.setEnabled(true);

        columnComparisonConfig = new ColumnComparisonConfig();
        columnComparisonConfig.setId(1L);
        columnComparisonConfig.setDayOverDayConfig(dayOverDayConfig);
        columnComparisonConfig.setColumnName("total_amount");
        columnComparisonConfig.setComparisonType(ComparisonType.PERCENTAGE);

        // Create validation detail results
        validationDetailResults = new ArrayList<>();

        ValidationDetailResult detail1 = new ValidationDetailResult();
        detail1.setId(1L);
        detail1.setValidationResult(validationResult);
        detail1.setColumnComparisonConfig(columnComparisonConfig);
        detail1.setThresholdExceeded(true);
        detail1.setActualValue(new BigDecimal("120.00"));
        detail1.setExpectedValue(new BigDecimal("100.00"));
        detail1.setDifferenceValue(new BigDecimal("20.00"));
        detail1.setDifferencePercentage(new BigDecimal("20.00"));
        validationDetailResults.add(detail1);

        ValidationDetailResult detail2 = new ValidationDetailResult();
        detail2.setId(2L);
        detail2.setValidationResult(validationResult);
        detail2.setColumnComparisonConfig(columnComparisonConfig);
        detail2.setThresholdExceeded(true);
        detail2.setActualValue(new BigDecimal("90.00"));
        detail2.setExpectedValue(new BigDecimal("100.00"));
        detail2.setDifferenceValue(new BigDecimal("-10.00"));
        detail2.setDifferencePercentage(new BigDecimal("-10.00"));
        validationDetailResults.add(detail2);
    }

    @Test
    public void testGetAllAlerts() throws Exception {
        when(validationDetailResultRepository.findAll()).thenReturn(validationDetailResults);

        mockMvc.perform(get("/api/v1/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].tableName", is("orders")))
                .andExpect(jsonPath("$[0].columnName", is("total_amount")))
                .andExpect(jsonPath("$[0].differencePercentage", is(20.0)))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].differencePercentage", is(-10.0)));

        verify(validationDetailResultRepository).findAll();
    }

    @Test
    public void testGetAlertsBySeverity() throws Exception {
        // Filter to only include HIGH severity alerts in a real implementation
        // For this test, we're simplifying and returning all alerts
        when(validationDetailResultRepository.findAll()).thenReturn(validationDetailResults);

        mockMvc.perform(get("/api/v1/alerts/severity/HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].tableName", is("orders")))
                .andExpect(jsonPath("$[0].severity", is("HIGH")));

        verify(validationDetailResultRepository).findAll();
    }

    @Test
    public void testGetAlertsBySeverity_InvalidSeverity() throws Exception {
        mockMvc.perform(get("/api/v1/alerts/severity/INVALID"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testAcknowledgeAlert() throws Exception {
        mockMvc.perform(put("/api/v1/alerts/1/acknowledge"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.acknowledged", is(true)))
                .andExpect(jsonPath("$.message", is("Alert acknowledged successfully")));
    }

    @Test
    public void testGetAlertCount() throws Exception {
        when(validationDetailResultRepository.findAll()).thenReturn(validationDetailResults);

        mockMvc.perform(get("/api/v1/alerts/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertCount", is(2)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));

        verify(validationDetailResultRepository).findAll();
    }
}
