package com.company.datavalidation.service.validation;

import com.company.datavalidation.model.ComparisonConfig;
import com.company.datavalidation.model.CrossTableConfig;
import com.company.datavalidation.model.DayOverDayConfig;
import com.company.datavalidation.model.ValidationResult;
import com.company.datavalidation.repository.ComparisonConfigRepository;
import com.company.datavalidation.repository.CrossTableConfigRepository;
import com.company.datavalidation.repository.DayOverDayConfigRepository;
import com.company.datavalidation.repository.ValidationResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Validation Executor Tests")
class ValidationExecutorTest {

    @Mock
    private ComparisonConfigRepository comparisonConfigRepository;

    @Mock
    private DayOverDayConfigRepository dayOverDayConfigRepository;

    @Mock
    private CrossTableConfigRepository crossTableConfigRepository;

    @Mock
    private ThresholdValidator thresholdValidator;

    @Mock
    private ValidationResultRepository validationResultRepository;

    @InjectMocks
    private ValidationExecutor validationExecutor;

    // Test objects
    private ComparisonConfig config1;
    private ComparisonConfig config2;
    private DayOverDayConfig dayOverDayConfig;
    private CrossTableConfig crossTableConfig;
    private ValidationResult successResult;
    private ValidationResult failedResult;

    @BeforeEach
    void setup() {
        // Setup comparison configs
        config1 = ComparisonConfig.builder()
                .id(1L)
                .tableName("table1")
                .enabled(true)
                .build();

        config2 = ComparisonConfig.builder()
                .id(2L)
                .tableName("table2")
                .enabled(true)
                .build();

        // Setup day-over-day config
        dayOverDayConfig = DayOverDayConfig.builder()
                .id(1L)
                .comparisonConfig(config1)
                .enabled(true)
                .build();

        // Setup cross-table config
        crossTableConfig = CrossTableConfig.builder()
                .id(1L)
                .sourceComparisonConfig(config2)
                .targetTableName("target_table")
                .enabled(true)
                .build();

        // Setup validation results
        successResult = ValidationResult.builder()
                .id(1L)
                .comparisonConfig(config1)
                .success(true)
                .build();

        failedResult = ValidationResult.builder()
                .id(2L)
                .comparisonConfig(config2)
                .success(false)
                .build();
    }

    @Test
    @DisplayName("Should execute all enabled validations")
    void testExecuteAllValidations() {
        // Mock repository behavior with lenient strictness
        lenient().when(comparisonConfigRepository.findByEnabled(true))
                .thenReturn(List.of(config1, config2));

        lenient().when(dayOverDayConfigRepository.findByComparisonConfigId(config1.getId()))
                .thenReturn(Optional.of(dayOverDayConfig));

        lenient().when(dayOverDayConfigRepository.findByComparisonConfigId(config2.getId()))
                .thenReturn(Optional.empty());

        // Use answer to match any ComparisonConfig
        lenient().when(crossTableConfigRepository.findBySourceComparisonConfigAndEnabled(any(ComparisonConfig.class), eq(true)))
                .thenAnswer((Answer<List<CrossTableConfig>>) invocation -> {
                    ComparisonConfig config = invocation.getArgument(0);
                    if (config.getId().equals(2L)) {
                        return List.of(crossTableConfig);
                    } else {
                        return List.of();
                    }
                });

        lenient().when(thresholdValidator.validateDayOverDay(dayOverDayConfig))
                .thenReturn(successResult);

        lenient().when(thresholdValidator.validateCrossTable(crossTableConfig))
                .thenReturn(failedResult);

        // Execute validations
        var results = validationExecutor.executeAllValidations();

        // Verify results
        assertNotNull(results);
        assertEquals(2, results.size());
        assertTrue(results.contains(successResult));
        assertTrue(results.contains(failedResult));

        // Verify repository calls
        verify(comparisonConfigRepository).findByEnabled(true);
        verify(dayOverDayConfigRepository, times(2)).findByComparisonConfigId(anyLong());
        verify(crossTableConfigRepository, times(2)).findBySourceComparisonConfigAndEnabled(any(ComparisonConfig.class), eq(true));
        verify(thresholdValidator).validateDayOverDay(dayOverDayConfig);
        verify(thresholdValidator).validateCrossTable(crossTableConfig);
    }

    @Test
    @DisplayName("Should execute validation for a specific table")
    void testExecuteValidationForTable() {
        // Mock repository behavior
        when(comparisonConfigRepository.findByTableNameIgnoreCase("table1"))
                .thenReturn(Optional.of(config1));

        when(dayOverDayConfigRepository.findByComparisonConfigId(config1.getId()))
                .thenReturn(Optional.of(dayOverDayConfig));

        when(crossTableConfigRepository.findBySourceComparisonConfigAndEnabled(config1, true))
                .thenReturn(List.of());

        when(thresholdValidator.validateDayOverDay(dayOverDayConfig))
                .thenReturn(successResult);

        // Execute validations
        var results = validationExecutor.executeValidationForTable("table1");

        // Verify results
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(successResult, results.getFirst());

        // Verify repository calls
        verify(comparisonConfigRepository).findByTableNameIgnoreCase("table1");
        verify(dayOverDayConfigRepository).findByComparisonConfigId(config1.getId());
        verify(crossTableConfigRepository).findBySourceComparisonConfigAndEnabled(config1, true);
        verify(thresholdValidator).validateDayOverDay(dayOverDayConfig);
    }

    @Test
    @DisplayName("Should return empty list when table is not found")
    void testExecuteValidationForTableNotFound() {
        // Mock repository behavior
        when(comparisonConfigRepository.findByTableNameIgnoreCase("nonexistent"))
                .thenReturn(Optional.empty());

        // Execute validations
        var results = validationExecutor.executeValidationForTable("nonexistent");

        // Verify results
        assertNotNull(results);
        assertTrue(results.isEmpty());

        // Verify repository calls
        verify(comparisonConfigRepository).findByTableNameIgnoreCase("nonexistent");
        verifyNoInteractions(dayOverDayConfigRepository);
        verifyNoInteractions(crossTableConfigRepository);
        verifyNoInteractions(thresholdValidator);
    }

    @Test
    @DisplayName("Should execute validation for a specific config")
    void testExecuteValidationForConfig() {
        // Mock repository behavior
        when(comparisonConfigRepository.findById(1L))
                .thenReturn(Optional.of(config1));

        when(dayOverDayConfigRepository.findByComparisonConfigId(config1.getId()))
                .thenReturn(Optional.of(dayOverDayConfig));

        when(crossTableConfigRepository.findBySourceComparisonConfigAndEnabled(config1, true))
                .thenReturn(List.of());

        when(thresholdValidator.validateDayOverDay(dayOverDayConfig))
                .thenReturn(successResult);

        // Execute validations
        var results = validationExecutor.executeValidationForConfig(1L);

        // Verify results
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(successResult, results.getFirst());

        // Verify repository calls
        verify(comparisonConfigRepository).findById(1L);
        verify(dayOverDayConfigRepository).findByComparisonConfigId(config1.getId());
        verify(crossTableConfigRepository).findBySourceComparisonConfigAndEnabled(config1, true);
        verify(thresholdValidator).validateDayOverDay(dayOverDayConfig);
    }

    // In ValidationExecutorTest.java
    @Test
    @DisplayName("Should handle exceptions during validation")
    void testExecuteValidationWithException() {
        // Mock repository behavior
        when(comparisonConfigRepository.findById(1L))
                .thenReturn(Optional.of(config1));

        when(dayOverDayConfigRepository.findByComparisonConfigId(config1.getId()))
                .thenReturn(Optional.of(dayOverDayConfig));

        // Simulate the exception
        when(thresholdValidator.validateDayOverDay(dayOverDayConfig))
                .thenThrow(new RuntimeException("Test exception"));

        // This might be the unnecessary stubbing - remove it if not needed
        // when(validationResultRepository.save(any(ValidationResult.class)))
        //    .thenAnswer(invocation -> invocation.getArgument(0));

        // Execute validations
        var results = validationExecutor.executeValidationForConfig(1L);

        // Verify results
        assertNotNull(results);
        assertEquals(1, results.size());

        var result = results.getFirst();
        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("Test exception"));

        // Verify repository calls - just the necessary ones
        verify(comparisonConfigRepository).findById(1L);
        verify(dayOverDayConfigRepository).findByComparisonConfigId(config1.getId());
        verify(thresholdValidator).validateDayOverDay(dayOverDayConfig);
        // Don't verify validationResultRepository.save if not essential for the test
    }

    @Test
    @DisplayName("Should retry a failed validation")
    void testRetryValidation() {
        // Mock repository behavior
        when(validationResultRepository.findById(1L))
                .thenReturn(Optional.of(failedResult));

        when(dayOverDayConfigRepository.findByComparisonConfigId(failedResult.getComparisonConfig().getId()))
                .thenReturn(Optional.empty());

        when(crossTableConfigRepository.findBySourceComparisonConfigAndEnabled(
                failedResult.getComparisonConfig(), true))
                .thenReturn(List.of(crossTableConfig));

        when(thresholdValidator.validateCrossTable(crossTableConfig))
                .thenReturn(successResult);

        // Execute retry
        var result = validationExecutor.retryValidation(1L);

        // Verify results
        assertNotNull(result);
        assertEquals(successResult, result);

        // Verify repository calls
        verify(validationResultRepository).findById(1L);
        verify(crossTableConfigRepository).findBySourceComparisonConfigAndEnabled(
                failedResult.getComparisonConfig(), true);
        verify(thresholdValidator).validateCrossTable(crossTableConfig);
    }

    @Test
    @DisplayName("Should return null when retry validation not found")
    void testRetryValidationNotFound() {
        // Mock repository behavior
        when(validationResultRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Execute retry
        var result = validationExecutor.retryValidation(999L);

        // Verify results
        assertNull(result);

        // Verify repository calls
        verify(validationResultRepository).findById(999L);
        verifyNoInteractions(dayOverDayConfigRepository);
        verifyNoInteractions(crossTableConfigRepository);
        verifyNoInteractions(thresholdValidator);
    }
}
