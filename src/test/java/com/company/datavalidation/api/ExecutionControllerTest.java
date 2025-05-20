package com.company.datavalidation.api;

import com.company.datavalidation.model.ComparisonConfig;
import com.company.datavalidation.model.ValidationResult;
import com.company.datavalidation.repository.ValidationDetailResultRepository;
import com.company.datavalidation.repository.ValidationResultRepository;
import com.company.datavalidation.service.validation.ValidationExecutor;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        // Initialize ObjectMapper with proper modules for date/time serialization
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // For handling LocalDateTime
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // Use ISO-8601 format
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL); // Ignore null fields

        // Set up MockMvc with the configured ObjectMapper
        mockMvc = MockMvcBuilders.standaloneSetup(executionController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();

        // Rest of your setup code remains the same...
        // Setup test data
        ComparisonConfig comparisonConfig = ComparisonConfig.builder()
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

    // Now the test method:
    @Test
    @DisplayName("Should get execution history")
    void testGetExecutionHistory() throws Exception {
        // Create a proper Page implementation with our test data
        List<ValidationResult> results = new ArrayList<>();
        results.add(validationResult1);
        results.add(validationResult2);

        // Detach the lazy-loaded entities to avoid serialization issues
        // or use a DTO projection in your actual code
        for (ValidationResult result : results) {
            // Make sure ComparisonConfig reference won't cause issues during serialization
            ComparisonConfig config = new ComparisonConfig();
            config.setId(result.getComparisonConfig().getId());
            config.setTableName(result.getComparisonConfig().getTableName());
            config.setEnabled(result.getComparisonConfig().isEnabled());
            config.setDescription(result.getComparisonConfig().getDescription());
            result.setComparisonConfig(config);
        }

        Page<ValidationResult> page = new PageImpl<>(results);

        PageRequest pageRequest = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "executionDate"));
        when(validationResultRepository.findAll(eq(pageRequest))).thenReturn(page);

        mockMvc.perform(get("/api/v1/executions")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id", is(1)))
                .andExpect(jsonPath("$.content[1].id", is(2)));

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
