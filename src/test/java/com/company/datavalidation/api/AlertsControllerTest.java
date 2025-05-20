package com.company.datavalidation.api;

import com.company.datavalidation.model.*;
import com.company.datavalidation.repository.ValidationDetailResultRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Alerts Controller Tests")
class AlertsControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ValidationDetailResultRepository validationDetailResultRepository;

    @InjectMocks
    private AlertsController alertsController;

    // Test data
    private List<ValidationDetailResult> validationDetailResults;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(alertsController).build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // For handling Java 8 date/time

        // Setup test data using builders
        ComparisonConfig comparisonConfig = ComparisonConfig.builder()
                .id(1L)
                .tableName("orders")
                .enabled(true)
                .build();

        ValidationResult validationResult = ValidationResult.builder()
                .id(1L)
                .comparisonConfig(comparisonConfig)
                .executionDate(LocalDateTime.now().minusHours(1))
                .success(false)
                .errorMessage("Validation failed: threshold exceeded")
                .build();

        DayOverDayConfig dayOverDayConfig = DayOverDayConfig.builder()
                .id(1L)
                .comparisonConfig(comparisonConfig)
                .enabled(true)
                .build();

        ColumnComparisonConfig columnComparisonConfig = ColumnComparisonConfig.builder()
                .id(1L)
                .dayOverDayConfig(dayOverDayConfig)
                .columnName("total_amount")
                .comparisonType(ComparisonType.PERCENTAGE)
                .nullHandlingStrategy(HandlingStrategy.TREAT_AS_ZERO)
                .blankHandlingStrategy(HandlingStrategy.TREAT_AS_ZERO)
                .naHandlingStrategy(HandlingStrategy.TREAT_AS_ZERO)
                .build();

        // Create validation detail results
        validationDetailResults = List.of(
                ValidationDetailResult.builder()
                        .id(1L)
                        .validationResult(validationResult)
                        .columnComparisonConfig(columnComparisonConfig)
                        .thresholdExceeded(true)
                        .actualValue(new BigDecimal("120.00"))
                        .expectedValue(new BigDecimal("100.00"))
                        .differenceValue(new BigDecimal("20.00"))
                        .differencePercentage(new BigDecimal("20.00"))
                        .build(),

                ValidationDetailResult.builder()
                        .id(2L)
                        .validationResult(validationResult)
                        .columnComparisonConfig(columnComparisonConfig)
                        .thresholdExceeded(true)
                        .actualValue(new BigDecimal("90.00"))
                        .expectedValue(new BigDecimal("100.00"))
                        .differenceValue(new BigDecimal("-10.00"))
                        .differencePercentage(new BigDecimal("-10.00"))
                        .build()
        );
    }

    @Test
    @DisplayName("Should get all alerts")
    void testGetAllAlerts() throws Exception {
        when(validationDetailResultRepository.findAll()).thenReturn(validationDetailResults);

        mockMvc.perform(get("/api/v1/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].tableName", is("orders")))
                .andExpect(jsonPath("$[0].columnName", is("total_amount")))
                .andExpect(jsonPath("$[0].differencePercentage", is("20.00%")))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].differencePercentage", is("-10.00%")));

        verify(validationDetailResultRepository).findAll();
    }

    @Test
    @DisplayName("Should get alerts by severity")
    void testGetAlertsBySeverity() throws Exception {
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
    @DisplayName("Should return bad request for invalid severity")
    void testGetAlertsBySeverityInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/alerts/severity/INVALID"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should acknowledge alert")
    void testAcknowledgeAlert() throws Exception {
        mockMvc.perform(put("/api/v1/alerts/1/acknowledge")
                        .param("acknowledgedBy", "test-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.acknowledged", is(true)))
                .andExpect(jsonPath("$.message", is("Alert acknowledged successfully")))
                .andExpect(jsonPath("$.acknowledgedBy", is("test-user")));
    }

    @Test
    @DisplayName("Should get alert count")
    void testGetAlertCount() throws Exception {
        when(validationDetailResultRepository.findAll()).thenReturn(validationDetailResults);

        mockMvc.perform(get("/api/v1/alerts/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertCount", is(2)))
                .andExpect(jsonPath("$.countBySeverity.HIGH", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(validationDetailResultRepository).findAll();
    }

    @Test
    @DisplayName("Should handle empty alert list")
    void testGetAlertsEmpty() throws Exception {
        when(validationDetailResultRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(validationDetailResultRepository).findAll();
    }
}
