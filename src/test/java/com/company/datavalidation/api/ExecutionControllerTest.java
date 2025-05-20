package com.company.datavalidation.api;

import com.company.datavalidation.model.ComparisonConfig;
import com.company.datavalidation.model.ValidationResult;
import com.company.datavalidation.repository.ValidationDetailResultRepository;
import com.company.datavalidation.repository.ValidationResultRepository;
import com.company.datavalidation.service.validation.ValidationExecutor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Execution Controller Tests")
class ExecutionControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ValidationExecutor validationExecutor;

    @Mock
    private ValidationResultRepository validationResultRepository;

    @Mock
    private ValidationDetailResultRepository validationDetailResultRepository;

    @InjectMocks
    private ExecutionController executionController;

    private ValidationResult validationResult1;
    private ValidationResult validationResult2;
    private ComparisonConfig comparisonConfig;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(executionController).build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // For handling Java 8 date/time

        // Setup test data
        comparisonConfig = ComparisonConfig.builder()
                .id(1L)
                .tableName("orders")
                .enabled(true)
                .description("Orders table validation")
                .createdDate(LocalDateTime.now())
                .lastModifiedDate(LocalDateTime.now())
                .build();

        validationResult1 = ValidationResult.builder()
                .id(1L)
                .comparisonConfig(comparisonConfig)
                .executionDate(LocalDateTime.now())
                .success(true)
                .executionTimeMs(150)
                .build();

        validationResult2 = ValidationResult.builder()
                .id(2L)
                .comparisonConfig(comparisonConfig)
                .executionDate(LocalDateTime.now().minusDays(1))
                .success(false)
                .errorMessage("Validation failed: threshold exceeded")
                .executionTimeMs(200)
                .build();
    }

    @Test
    @DisplayName("Should execute all validations")
    void testExecuteAllValidations() throws Exception {
        when(validationExecutor.executeAllValidations()).thenReturn(List.of(validationResult1, validationResult2));

        mockMvc.perform(post("/api/v1/executions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].success", is(true)))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].success", is(false)));

        verify(validationExecutor).executeAllValidations();
    }

    @Test
    @DisplayName("Should execute validation for table")
    void testExecuteValidationForTable() throws Exception {
        when(validationExecutor.executeValidationForTable("orders")).thenReturn(List.of(validationResult1));

        mockMvc.perform(post("/api/v1/executions/tables/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].success", is(true)));

        verify(validationExecutor).executeValidationForTable("orders");
    }

    @Test
    @DisplayName("Should execute validation for config")
    void testExecuteValidationForConfig() throws Exception {
        when(validationExecutor.executeValidationForConfig(1L)).thenReturn(List.of(validationResult1));

        mockMvc.perform(post("/api/v1/executions/configs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].success", is(true)));

        verify(validationExecutor).executeValidationForConfig(1L);
    }

    @Test
    @DisplayName("Should get execution history")
    void testGetExecutionHistory() throws Exception {
        List<ValidationResult> results = List.of(validationResult1, validationResult2);
        Page<ValidationResult> page = new PageImpl<>(results);

        PageRequest pageRequest = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "executionDate"));
        when(validationResultRepository.findAll(eq(pageRequest))).thenReturn(page);

        mockMvc.perform(get("/api/v1/executions")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id", is(1)))
                .andExpect(jsonPath("$.content[1].id", is(2)))
                .andExpect(jsonPath("$.totalElements", is(2)));

        verify(validationResultRepository).findAll(eq(pageRequest));
    }

    @Test
    @DisplayName("Should get execution details")
    void testGetExecutionDetails() throws Exception {
        when(validationResultRepository.findById(1L)).thenReturn(Optional.of(validationResult1));

        mockMvc.perform(get("/api/v1/executions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.executionTimeMs", is(150)));

        verify(validationResultRepository).findById(1L);
    }

    @Test
    @DisplayName("Should return 404 when execution details not found")
    void testGetExecutionDetailsNotFound() throws Exception {
        when(validationResultRepository.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/executions/1"))
                .andExpect(status().isNotFound());

        verify(validationResultRepository).findById(1L);
    }

    @Test
    @DisplayName("Should get detailed results")
    void testGetDetailedResults() throws Exception {
        // Setup the validation detail results
        when(validationResultRepository.findById(1L)).thenReturn(Optional.of(validationResult1));
        // For simplicity, we're not setting up the actual validation detail results
        // In a real test, you would mock the repository to return actual ValidationDetailResults

        mockMvc.perform(get("/api/v1/executions/1/results"))
                .andExpect(status().isOk());
        // In a real test, you would verify the response content
    }

    @Test
    @DisplayName("Should retry validation")
    void testRetryValidation() throws Exception {
        when(validationExecutor.retryValidation(1L)).thenReturn(validationResult1);

        mockMvc.perform(post("/api/v1/executions/1/retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.success", is(true)));

        verify(validationExecutor).retryValidation(1L);
    }

    @Test
    @DisplayName("Should return 404 when retry validation not found")
    void testRetryValidationNotFound() throws Exception {
        when(validationExecutor.retryValidation(1L)).thenReturn(null);

        mockMvc.perform(post("/api/v1/executions/1/retry"))
                .andExpect(status().isNotFound());

        verify(validationExecutor).retryValidation(1L);
    }

    @Test
    @DisplayName("Should get validation summary")
    void testGetExecutionSummary() throws Exception {
        // Mock the necessary data for validation summary
        when(validationResultRepository.count()).thenReturn(100L);
        when(validationResultRepository.countBySuccessAndExecutionDateAfter(eq(true), any(LocalDateTime.class)))
                .thenReturn(90L);
        when(validationResultRepository.countBySuccessAndExecutionDateAfter(eq(false), any(LocalDateTime.class)))
                .thenReturn(10L);

        // Mock finding the latest execution
        when(validationResultRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(validationResult1)));

        mockMvc.perform(get("/api/v1/executions/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExecutions", is(100)))
                .andExpect(jsonPath("$.successfulExecutions", is(90)))
                .andExpect(jsonPath("$.failedExecutions", is(10)))
                .andExpect(jsonPath("$.successRate", is(90.0)))
                .andExpect(jsonPath("$.lastExecutionTime").exists());
    }
}
