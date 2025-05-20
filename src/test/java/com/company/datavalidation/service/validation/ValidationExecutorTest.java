package com.company.datavalidation.service.validation;

import com.company.datavalidation.model.*;
import com.company.datavalidation.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ValidationExecutorTest {

    @Mock
    private ComparisonConfigRepository comparisonConfigRepository;

    @Mock
    private DayOverDayConfigRepository dayOverDayConfigRepository;

    @Mock
    private CrossTableConfigRepository crossTableConfigRepository;

    @Mock
    private ThresholdValidator thresholdValidator;

    @InjectMocks
    private ValidationExecutor validationExecutor;

    private ComparisonConfig config1;
    private ComparisonConfig config2;
    private DayOverDayConfig dayOverDayConfig;
    private CrossTableConfig crossTableConfig;
    private ValidationResult successResult;
    private ValidationResult failedResult;

    @BeforeEach
    public void setup() {
        // Setup comparison configs
        config1 = new ComparisonConfig();
        config1.setId(1L);
        config1.setTableName("table1");
        config1.setEnabled(true);

        config2 = new ComparisonConfig();
        config2.setId(2L);
        config2.setTableName("table2");
        config2.setEnabled(true);

        // Setup day-over-day config
        dayOverDayConfig = new DayOverDayConfig();
        dayOverDayConfig.setId(1L);
        dayOverDayConfig.setComparisonConfig(config1);
        dayOverDayConfig.setEnabled(true);

        // Setup cross-table config
        crossTableConfig = new CrossTableConfig();
        crossTableConfig.setId(1L);
        crossTableConfig.setSourceComparisonConfig(config2);
        crossTableConfig.setTargetTableName("target_table");
        crossTableConfig.setEnabled(true);

        // Setup validation results
        successResult = new ValidationResult();
        successResult.setId(1L);
        successResult.setComparisonConfig(config1);
        successResult.setSuccess(true);

        failedResult = new ValidationResult();
        failedResult.setId(2L);
        failedResult.setComparisonConfig(config2);
        failedResult.setSuccess(false);
    }

    @Test
    public void testExecuteAllValidations() {
        // Mock repository behavior
        when(comparisonConfigRepository.findByEnabled(true))
                .thenReturn(Arrays.asList(config1, config2));

        when(dayOverDayConfigRepository.findByComparisonConfigId(config1.getId()))
                .thenReturn(Optional.of(dayOverDayConfig));

        when(crossTableConfigRepository.findBySourceComparisonConfigAndEnabled(config2, true))
                .thenReturn(Collections.singletonList(crossTableConfig));

        when(thresholdValidator.validateDayOverDay(dayOverDayConfig))
                .thenReturn(successResult);

        when(thresholdValidator.validateCrossTable(crossTableConfig))
                .thenReturn(failedResult);

        // Execute validations
        List<ValidationResult> results = validationExecutor.executeAllValidations();

        // Verify results
        assertNotNull(results);
        assertEquals(2, results.size());
        assertTrue(results.contains(successResult));
        assertTrue(results.contains(failedResult));

        // Verify repository calls
        verify(comparisonConfigRepository).findByEnabled(true);
        verify(dayOverDayConfigRepository).findByComparisonConfigId(config1.getId());
        verify(crossTableConfigRepository).findBySourceComparisonConfigAndEnabled(config2, true);
        verify(thresholdValidator).validateDayOverDay(dayOverDayConfig);
        verify(thresholdValidator).validateCrossTable(crossTableConfig);
    }

    @Test
    public void testExecuteValidationForTable() {
        // Mock repository behavior
        when(comparisonConfigRepository.findByTableNameIgnoreCase("table1"))
                .thenReturn(Optional.of(config1));

        when(dayOverDayConfigRepository.findByComparisonConfigId(config1.getId()))
                .thenReturn(Optional.of(dayOverDayConfig));

        when(crossTableConfigRepository.findBySourceComparisonConfigAndEnabled(config1, true))
                .thenReturn(Collections.emptyList());

        when(thresholdValidator.validateDayOverDay(dayOverDayConfig))
                .thenReturn(successResult);

        // Execute validations
        List<ValidationResult> results = validationExecutor.executeValidationForTable("table1");

        // Verify results
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(successResult, results.get(0));

        // Verify repository calls
        verify(comparisonConfigRepository).findByTableNameIgnoreCase("table1");
        verify(dayOverDayConfigRepository).findByComparisonConfigId(config1.getId());
        verify(crossTableConfigRepository).findBySourceComparisonConfigAndEnabled(config1, true);
        verify(thresholdValidator).validateDayOverDay(dayOverDayConfig);
    }

    @Test
    public void testExecuteValidationForConfig() {
        // Mock repository behavior
        when(comparisonConfigRepository.findById(1L))
                .thenReturn(Optional.of(config1));

        when(dayOverDayConfigRepository.findByComparisonConfigId(config1.getId()))
                .thenReturn(Optional.of(dayOverDayConfig));

        when(crossTableConfigRepository.findBySourceComparisonConfigAndEnabled(config1, true))
                .thenReturn(Collections.emptyList());

        when(thresholdValidator.validateDayOverDay(dayOverDayConfig))
                .thenReturn(successResult);

        // Execute validations
        List<ValidationResult> results = validationExecutor.executeValidationForConfig(1L);

        // Verify results
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(successResult, results.get(0));

        // Verify repository calls
        verify(comparisonConfigRepository).findById(1L);
        verify(dayOverDayConfigRepository).findByComparisonConfigId(config1.getId());
        verify(crossTableConfigRepository).findBySourceComparisonConfigAndEnabled(config1, true);
        verify(thresholdValidator).validateDayOverDay(dayOverDayConfig);
    }
}
