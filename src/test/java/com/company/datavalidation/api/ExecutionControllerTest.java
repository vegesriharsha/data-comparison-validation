package com.company.datavalidation.api;

import com.company.datavalidation.model.ComparisonConfig;
import com.company.datavalidation.model.ValidationResult;
import com.company.datavalidation.repository.ValidationDetailResultRepository;
import com.company.datavalidation.repository.ValidationResultRepository;
import com.company.datavalidation.service.validation.ValidationExecutor;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class ExecutionControllerTest {

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
    private ComparisonConfig comparisonConfig;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(executionController).build();

        // Setup test data
        comparisonConfig = new ComparisonConfig();
        comparisonConfig.setId(1L);
        comparisonConfig.setTableName("orders");
        comparisonConfig.setEnabled(true);
        comparisonConfig.setDescription("Orders table validation");
        comparisonConfig.setCreatedDate(LocalDateTime.now());
        comparisonConfig.setLastModifiedDate(LocalDateTime.now());

        validationResult1 = new ValidationResult();
        validationResult1.setId(1L);
        validationResult1.setComparisonConfig(comparisonConfig);
        validationResult1.setExecutionDate(LocalDateTime.now());
        validationResult1.setSuccess(true);
        validationResult1.setExecutionTimeMs(150);

        validationResult2 = new ValidationResult();
        validationResult2.setId(2L);
        validationResult2.setComparisonConfig(comparisonConfig);
        validationResult2.setExecutionDate(LocalDateTime.now().minusDays(1));
        validationResult2.setSuccess(false);
        validationResult2.setErrorMessage("Validation failed: threshold exceeded");
        validationResult2.setExecutionTimeMs(200);
    }

    @Test
    public void testExecuteAllValidations() throws Exception {
        List<ValidationResult> results = Arrays.asList(validationResult1, validationResult2);
        when(validationExecutor.executeAllValidations()).thenReturn(results);

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
    public void testExecuteValidationForTable() throws Exception {
        List<ValidationResult> results = Arrays.asList(validationResult1);
        when(validationExecutor.executeValidationForTable("orders")).thenReturn(results);

        mockMvc.perform(post("/api/v1/executions/tables/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].success", is(true)));

        verify(validationExecutor).executeValidationForTable("orders");
    }

    @Test
    public void testExecuteValidationForConfig() throws Exception {
        List<ValidationResult> results = Arrays.asList(validationResult1);
        when(validationExecutor.executeValidationForConfig(1L)).thenReturn(results);

        mockMvc.perform(post("/api/v1/executions/configs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].success", is(true)));

        verify(validationExecutor).executeValidationForConfig(1L);
    }

    @Test
    public void testGetExecutionHistory() throws Exception {
        List<ValidationResult> results = Arrays.asList(validationResult1, validationResult2);
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
    public void testGetExecutionDetails() throws Exception {
        when(validationResultRepository.findById(1L)).thenReturn(Optional.of(validationResult1));

        mockMvc.perform(get("/api/v1/executions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.executionTimeMs", is(150)));

        verify(validationResultRepository).findById(1L);
    }

    @Test
    public void testGetExecutionDetails_NotFound() throws Exception {
        when(validationResultRepository.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/executions/1"))
                .andExpect(status().isNotFound());

        verify(validationResultRepository).findById(1L);
    }

    @Test
    public void testGetDetailedResults() throws Exception {
        // This is a simplified test since the actual implementation would use a custom DTO mapper
        mockMvc.perform(get("/api/v1/executions/1/results"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].message", is("Detailed results would be returned here")));
    }

    @Test
    public void testRetryValidation() throws Exception {
        when(validationExecutor.retryValidation(1L)).thenReturn(validationResult1);

        mockMvc.perform(post("/api/v1/executions/1/retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.success", is(true)));

        verify(validationExecutor).retryValidation(1L);
    }

    @Test
    public void testRetryValidation_NotFound() throws Exception {
        when(validationExecutor.retryValidation(1L)).thenReturn(null);

        mockMvc.perform(post("/api/v1/executions/1/retry"))
                .andExpect(status().isNotFound());

        verify(validationExecutor).retryValidation(1L);
    }
}
