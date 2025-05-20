package com.company.datavalidation.service.comparison;

import com.company.datavalidation.model.*;
import com.company.datavalidation.repository.DynamicTableRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CrossTableComparatorTest {

    @Mock
    private DynamicTableRepository dynamicTableRepository;

    @InjectMocks
    private CrossTableComparator crossTableComparator;

    private ComparisonConfig sourceConfig;
    private CrossTableConfig crossTableConfig;
    private List<ColumnComparisonConfig> columnConfigs;
    private Map<Long, ThresholdConfig> thresholdConfigs;

    @BeforeEach
    public void setup() {
        // Setup source comparison config
        sourceConfig = new ComparisonConfig();
        sourceConfig.setId(1L);
        sourceConfig.setTableName("source_table");
        sourceConfig.setEnabled(true);

        // Setup cross-table config
        crossTableConfig = new CrossTableConfig();
        crossTableConfig.setId(1L);
        crossTableConfig.setSourceComparisonConfig(sourceConfig);
        crossTableConfig.setTargetTableName("target_table");
        crossTableConfig.setJoinCondition("source_table.id = target_table.source_id");
        crossTableConfig.setEnabled(true);

        // Setup column configs
        columnConfigs = new ArrayList<>();

        ColumnComparisonConfig column1 = new ColumnComparisonConfig();
        column1.setId(1L);
        column1.setCrossTableConfig(crossTableConfig);
        column1.setColumnName("amount");
        column1.setTargetColumnName("amount");
        column1.setComparisonType(ComparisonType.PERCENTAGE);
        column1.setNullHandlingStrategy(HandlingStrategy.TREAT_AS_ZERO);
        column1.setBlankHandlingStrategy(HandlingStrategy.TREAT_AS_ZERO);
        column1.setNaHandlingStrategy(HandlingStrategy.TREAT_AS_ZERO);
        columnConfigs.add(column1);

        ColumnComparisonConfig column2 = new ColumnComparisonConfig();
        column2.setId(2L);
        column2.setCrossTableConfig(crossTableConfig);
        column2.setColumnName("count");
        column2.setTargetColumnName("total_count");
        column2.setComparisonType(ComparisonType.ABSOLUTE);
        column2.setNullHandlingStrategy(HandlingStrategy.TREAT_AS_ZERO);
        column2.setBlankHandlingStrategy(HandlingStrategy.TREAT_AS_ZERO);
        column2.setNaHandlingStrategy(HandlingStrategy.TREAT_AS_ZERO);
        columnConfigs.add(column2);

        // Setup threshold configs
        thresholdConfigs = new HashMap<>();

        ThresholdConfig threshold1 = new ThresholdConfig();
        threshold1.setId(1L);
        threshold1.setColumnComparisonConfig(column1);
        threshold1.setThresholdValue(new BigDecimal("5.00")); // 5% threshold
        threshold1.setSeverity(Severity.HIGH);
        threshold1.setNotificationEnabled(true);
        thresholdConfigs.put(column1.getId(), threshold1);

        ThresholdConfig threshold2 = new ThresholdConfig();
        threshold2.setId(2L);
        threshold2.setColumnComparisonConfig(column2);
        threshold2.setThresholdValue(new BigDecimal("3.00")); // 3 units threshold
        threshold2.setSeverity(Severity.MEDIUM);
        threshold2.setNotificationEnabled(true);
        thresholdConfigs.put(column2.getId(), threshold2);
    }

    @Test
    public void testCompare_NoThresholdExceeded() {
        // Prepare mock data
        List<String> sourceColumns = Arrays.asList("amount", "count");
        List<String> targetColumns = Arrays.asList("amount", "total_count");

        // Cross-table data
        List<Map<String, Object>> crossTableData = new ArrayList<>();
        Map<String, Object> row1 = new HashMap<>();
        row1.put("s_amount", new BigDecimal("100.00"));
        row1.put("t_amount", new BigDecimal("99.00"));
        row1.put("s_count", 10);
        row1.put("t_total_count", 9);
        crossTableData.add(row1);

        Map<String, Object> row2 = new HashMap<>();
        row2.put("s_amount", new BigDecimal("200.00"));
        row2.put("t_amount", new BigDecimal("198.00"));
        row2.put("s_count", 20);
        row2.put("t_total_count", 19);
        crossTableData.add(row2);

        // Mock repository behavior
        when(dynamicTableRepository.executeCrossTableQuery(
                eq("source_table"), eq("target_table"),
                eq(sourceColumns), eq(targetColumns),
                eq("source_table.id = target_table.source_id"),
                eq("created_date"), isNull()))
                .thenReturn(crossTableData);

        // Execute comparison
        List<ValidationDetailResult> results = crossTableComparator.compare(crossTableConfig, columnConfigs, thresholdConfigs);

        // Verify results
        assertEquals(4, results.size()); // 2 columns x 2 rows

        // Verify no thresholds were exceeded
        assertFalse(results.stream().anyMatch(ValidationDetailResult::isThresholdExceeded));

        // Verify amount comparison for first row
        ValidationDetailResult amountResult1 = results.stream()
                .filter(r -> r.getColumnComparisonConfig().getColumnName().equals("amount") &&
                        r.getActualValue().equals(new BigDecimal("100.00")))
                .findFirst()
                .orElseThrow();

        assertEquals(new BigDecimal("100.00"), amountResult1.getActualValue());
        assertEquals(new BigDecimal("99.00"), amountResult1.getExpectedValue());
        assertEquals(new BigDecimal("1.00"), amountResult1.getDifferenceValue());
        // Difference percentage should be about 1.01%
        assertTrue(amountResult1.getDifferencePercentage().compareTo(new BigDecimal("1.0")) > 0);
        assertTrue(amountResult1.getDifferencePercentage().compareTo(new BigDecimal("1.02")) < 0);
        assertFalse(amountResult1.isThresholdExceeded()); // 1.01% < 5% threshold

        // Verify count comparison for first row
        ValidationDetailResult countResult1 = results.stream()
                .filter(r -> r.getColumnComparisonConfig().getColumnName().equals("count") &&
                        r.getActualValue().equals(new BigDecimal("10")))
                .findFirst()
                .orElseThrow();

        assertEquals(new BigDecimal("10"), countResult1.getActualValue());
        assertEquals(new BigDecimal("9"), countResult1.getExpectedValue());
        assertEquals(new BigDecimal("1"), countResult1.getDifferenceValue());
        assertFalse(countResult1.isThresholdExceeded()); // 1 < 3 threshold
    }

    @Test
    public void testCompare_ThresholdExceeded() {
        // Prepare mock data
        List<String> sourceColumns = Arrays.asList("amount", "count");
        List<String> targetColumns = Arrays.asList("amount", "total_count");

        // Cross-table data
        List<Map<String, Object>> crossTableData = new ArrayList<>();
        Map<String, Object> row1 = new HashMap<>();
        row1.put("s_amount", new BigDecimal("100.00"));
        row1.put("t_amount", new BigDecimal("90.00"));
        row1.put("s_count", 10);
        row1.put("t_total_count", 6);
        crossTableData.add(row1);

        // Mock repository behavior
        when(dynamicTableRepository.executeCrossTableQuery(
                eq("source_table"), eq("target_table"),
                eq(sourceColumns), eq(targetColumns),
                eq("source_table.id = target_table.source_id"),
                eq("created_date"), isNull()))
                .thenReturn(crossTableData);

        // Execute comparison
        List<ValidationDetailResult> results = crossTableComparator.compare(crossTableConfig, columnConfigs, thresholdConfigs);

        // Verify results
        assertEquals(2, results.size()); // 2 columns x 1 row

        // Verify thresholds were exceeded
        assertTrue(results.stream().allMatch(ValidationDetailResult::isThresholdExceeded));

        // Verify amount comparison
        ValidationDetailResult amountResult = results.stream()
                .filter(r -> r.getColumnComparisonConfig().getColumnName().equals("amount"))
                .findFirst()
                .orElseThrow();

        assertEquals(new BigDecimal("100.00"), amountResult.getActualValue());
        assertEquals(new BigDecimal("90.00"), amountResult.getExpectedValue());
        assertEquals(new BigDecimal("10.00"), amountResult.getDifferenceValue());
        // Difference percentage should be about 11.11%
        assertTrue(amountResult.getDifferencePercentage().compareTo(new BigDecimal("11.1")) > 0);
        assertTrue(amountResult.getDifferencePercentage().compareTo(new BigDecimal("11.12")) < 0);
        assertTrue(amountResult.isThresholdExceeded()); // 11.11% > 5% threshold

        // Verify count comparison
        ValidationDetailResult countResult = results.stream()
                .filter(r -> r.getColumnComparisonConfig().getColumnName().equals("count"))
                .findFirst()
                .orElseThrow();

        assertEquals(new BigDecimal("10"), countResult.getActualValue());
        assertEquals(new BigDecimal("6"), countResult.getExpectedValue());
        assertEquals(new BigDecimal("4"), countResult.getDifferenceValue());
        assertTrue(countResult.isThresholdExceeded()); // 4 > 3 threshold
    }
}
