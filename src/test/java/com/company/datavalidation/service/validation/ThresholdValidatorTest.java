package com.company.datavalidation.service.validation;

import com.company.datavalidation.model.*;
import com.company.datavalidation.repository.*;
import com.company.datavalidation.service.comparison.CrossTableComparator;
import com.company.datavalidation.service.comparison.DayOverDayComparator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ThresholdValidatorTest {

    @Mock
    private DayOverDayComparator dayOverDayComparator;

    @Mock
    private CrossTableComparator crossTableComparator;

    @Mock
    private ColumnComparisonConfigRepository columnComparisonConfigRepository;

    @Mock
    private ThresholdConfigRepository thresholdConfigRepository;

    @Mock
    private ValidationResultRepository validationResultRepository;

    @Mock
    private ValidationDetailResultRepository validationDetailResultRepository;

    @InjectMocks
    private ThresholdValidator thresholdValidator;

    private ComparisonConfig comparisonConfig;
    private DayOverDayConfig dayOverDayConfig;
    private CrossTableConfig crossTableConfig;
    private List<ColumnComparisonConfig> columnConfigs;
    private List<ThresholdConfig> thresholdConfigs;

    @BeforeEach
    public void setup() {
        // Setup comparison config
        comparisonConfig = new ComparisonConfig();
        comparisonConfig.setId(1L);
        comparisonConfig.setTableName("test_table");
        comparisonConfig.setEnabled(true);

        // Setup day-over-day config
        dayOverDayConfig = new DayOverDayConfig();
        dayOverDayConfig.setId(1L);
        dayOverDayConfig.setComparisonConfig(comparisonConfig);
        dayOverDayConfig.setEnabled(true);

        // Setup cross-table config
        crossTableConfig = new CrossTableConfig();
        crossTableConfig.setId(2L);
        crossTableConfig.setSourceComparisonConfig(comparisonConfig);
        crossTableConfig.setTargetTableName("target_table");
        crossTableConfig.setJoinCondition("test_table.id = target_table.test_id");
        crossTableConfig.setEnabled(true);

        // Setup column configs
        columnConfigs = new ArrayList<>();

        ColumnComparisonConfig column1 = new ColumnComparisonConfig();
        column1.setId(1L);
        column1.setDayOverDayConfig(dayOverDayConfig);
        column1.setColumnName("amount");
        column1.setComparisonType(ComparisonType.PERCENTAGE);
        columnConfigs.add(column1);

        ColumnComparisonConfig column2 = new ColumnComparisonConfig();
        column2.setId(2L);
        column2.setCrossTableConfig(crossTableConfig);
        column2.setColumnName("count");
        column2.setTargetColumnName("total_count");
        column2.setComparisonType(ComparisonType.ABSOLUTE);
        columnConfigs.add(column2);

        // Setup threshold configs
        thresholdConfigs = new ArrayList<>();

        ThresholdConfig threshold1 = new ThresholdConfig();
        threshold1.setId(1L);
        threshold1.setColumnComparisonConfig(column1);
        threshold1.setThresholdValue(new BigDecimal("5.00"));
        thresholdConfigs.add(threshold1);

        ThresholdConfig threshold2 = new ThresholdConfig();
        threshold2.setId(2L);
        threshold2.setColumnComparisonConfig(column2);
        threshold2.setThresholdValue(new BigDecimal("3.00"));
        thresholdConfigs.add(threshold2);
    }

    @Test
    public void testValidateDayOverDay_Success() {
        // Prepare mock data
        List<ValidationDetailResult> detailResults = new ArrayList<>();

        ValidationDetailResult detail = new ValidationDetailResult();
        detail.setColumnComparisonConfig(columnConfigs.get(0));
        detail.setActualValue(new BigDecimal("100.00"));
        detail.setExpectedValue(new BigDecimal("98.00"));
        detail.setDifferenceValue(new BigDecimal("2.00"));
        detail.setDifferencePercentage(new BigDecimal("2.04"));
        detail.setThresholdExceeded(false); // Below threshold
        detailResults.add(detail);

        // Mock repository behavior
        when(columnComparisonConfigRepository.findByDayOverDayConfig(dayOverDayConfig))
                .thenReturn(Collections.singletonList(columnConfigs.get(0)));

        when(thresholdConfigRepository.findByColumnComparisonConfigId(anyLong()))
                .thenReturn(Collections.singletonList(thresholdConfigs.get(0)));

        when(dayOverDayComparator.compare(eq(dayOverDayConfig), anyList(), anyMap()))
                .thenReturn(detailResults);

        when(validationResultRepository.save(any(ValidationResult.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Execute validation
        ValidationResult result = thresholdValidator.validateDayOverDay(dayOverDayConfig);

        // Verify result
        assertNotNull(result);
        assertTrue(result.isSuccess()); // No thresholds exceeded
        assertEquals(comparisonConfig, result.getComparisonConfig());

        // Verify repository calls
        verify(columnComparisonConfigRepository).findByDayOverDayConfig(dayOverDayConfig);
        verify(thresholdConfigRepository).findByColumnComparisonConfigId(columnConfigs.get(0).getId());
        verify(dayOverDayComparator).compare(eq(dayOverDayConfig), anyList(), anyMap());
        verify(validationResultRepository, times(2)).save(any(ValidationResult.class));
        verify(validationDetailResultRepository).save(any(ValidationDetailResult.class));
    }

    @Test
    public void testValidateDayOverDay_ThresholdExceeded() {
        // Prepare mock data
        List<ValidationDetailResult> detailResults = new ArrayList<>();

        ValidationDetailResult detail = new ValidationDetailResult();
        detail.setColumnComparisonConfig(columnConfigs.get(0));
        detail.setActualValue(new BigDecimal("110.00"));
        detail.setExpectedValue(new BigDecimal("100.00"));
        detail.setDifferenceValue(new BigDecimal("10.00"));
        detail.setDifferencePercentage(new BigDecimal("10.00"));
        detail.setThresholdExceeded(true); // Above threshold
        detailResults.add(detail);

        // Mock repository behavior
        when(columnComparisonConfigRepository.findByDayOverDayConfig(dayOverDayConfig))
                .thenReturn(Collections.singletonList(columnConfigs.get(0)));

        when(thresholdConfigRepository.findByColumnComparisonConfigId(anyLong()))
                .thenReturn(Collections.singletonList(thresholdConfigs.get(0)));

        when(dayOverDayComparator.compare(eq(dayOverDayConfig), anyList(), anyMap()))
                .thenReturn(detailResults);

        when(validationResultRepository.save(any(ValidationResult.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Execute validation
        ValidationResult result = thresholdValidator.validateDayOverDay(dayOverDayConfig);

        // Verify result
        assertNotNull(result);
        assertFalse(result.isSuccess()); // Threshold exceeded
        assertEquals(comparisonConfig, result.getComparisonConfig());

        // Verify repository calls
        verify(columnComparisonConfigRepository).findByDayOverDayConfig(dayOverDayConfig);
        verify(thresholdConfigRepository).findByColumnComparisonConfigId(columnConfigs.get(0).getId());
        verify(dayOverDayComparator).compare(eq(dayOverDayConfig), anyList(), anyMap());
        verify(validationResultRepository, times(2)).save(any(ValidationResult.class));
        verify(validationDetailResultRepository).save(any(ValidationDetailResult.class));
    }

    @Test
    public void testValidateCrossTable_Success() {
        // Prepare mock data
        List<ValidationDetailResult> detailResults = new ArrayList<>();

        ValidationDetailResult detail = new ValidationDetailResult();
        detail.setColumnComparisonConfig(columnConfigs.get(1));
        detail.setActualValue(new BigDecimal("10"));
        detail.setExpectedValue(new BigDecimal("8"));
        detail.setDifferenceValue(new BigDecimal("2"));
        detail.setThresholdExceeded(false); // Below threshold
        detailResults.add(detail);

        // Mock repository behavior
        when(columnComparisonConfigRepository.findByCrossTableConfig(crossTableConfig))
                .thenReturn(Collections.singletonList(columnConfigs.get(1)));

        when(thresholdConfigRepository.findByColumnComparisonConfigId(anyLong()))
                .thenReturn(Collections.singletonList(thresholdConfigs.get(1)));

        when(crossTableComparator.compare(eq(crossTableConfig), anyList(), anyMap()))
                .thenReturn(detailResults);

        when(validationResultRepository.save(any(ValidationResult.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Execute validation
        ValidationResult result = thresholdValidator.validateCrossTable(crossTableConfig);

        // Verify result
        assertNotNull(result);
        assertTrue(result.isSuccess()); // No thresholds exceeded
        assertEquals(comparisonConfig, result.getComparisonConfig());

        // Verify repository calls
        verify(columnComparisonConfigRepository).findByCrossTableConfig(crossTableConfig);
        verify(thresholdConfigRepository).findByColumnComparisonConfigId(columnConfigs.get(1).getId());
        verify(crossTableComparator).compare(eq(crossTableConfig), anyList(), anyMap());
        verify(validationResultRepository, times(2)).save(any(ValidationResult.class));
        verify(validationDetailResultRepository).save(any(ValidationDetailResult.class));
    }
}
