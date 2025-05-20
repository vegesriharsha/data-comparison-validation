package com.company.datavalidation.service.comparison;

import com.company.datavalidation.model.*;
import com.company.datavalidation.repository.DynamicTableRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
@DisplayName("Cross Table Comparator Tests")
class CrossTableComparatorTest {

    @Mock
    private DynamicTableRepository dynamicTableRepository;

    @InjectMocks
    private CrossTableComparator crossTableComparator;

    // Test objects
    private ComparisonConfig sourceConfig;
    private CrossTableConfig crossTableConfig;
    private List<ColumnComparisonConfig> columnConfigs;
    private Map<Long, ThresholdConfig> thresholdConfigs;

    @BeforeEach
    void setup() {
        // Setup source comparison config
        sourceConfig = ComparisonConfig.builder()
                .id(1L)
                .tableName("source_table")
                .enabled(true)
                .build();

        // Setup cross-table config
        crossTableConfig = CrossTableConfig.builder()
                .id(1L)
                .sourceComparisonConfig(sourceConfig)
                .targetTableName("target_table")
                .joinCondition("source_table.id = target_table.source_id")
                .enabled(true)
                .build();

        // Setup column configs
        columnConfigs = new ArrayList<>();

        var column1 = ColumnComparisonConfig.builder()
                .id(1L)
                .crossTableConfig(crossTableConfig)
                .columnName("amount")
                .targetColumnName("amount")
                .comparisonType(ComparisonType.PERCENTAGE)
                .nullHandlingStrategy(HandlingStrategy.TREAT_AS_ZERO)
                .blankHandlingStrategy(HandlingStrategy.TREAT_AS_ZERO)
                .naHandlingStrategy(HandlingStrategy.TREAT_AS_ZERO)
                .build();
        columnConfigs.add(column1);

        var column2 = ColumnComparisonConfig.builder()
                .id(2L)
                .crossTableConfig(crossTableConfig)
                .columnName("count")
                .targetColumnName("total_count")
                .comparisonType(ComparisonType.ABSOLUTE)
                .nullHandlingStrategy(HandlingStrategy.TREAT_AS_ZERO)
                .blankHandlingStrategy(HandlingStrategy.TREAT_AS_ZERO)
                .naHandlingStrategy(HandlingStrategy.TREAT_AS_ZERO)
                .build();
        columnConfigs.add(column2);

        // Setup threshold configs
        thresholdConfigs = new HashMap<>();

        var threshold1 = ThresholdConfig.builder()
                .id(1L)
                .columnComparisonConfig(column1)
                .thresholdValue(new BigDecimal("5.00")) // 5% threshold
                .severity(Severity.HIGH)
                .notificationEnabled(true)
                .build();
        thresholdConfigs.put(column1.getId(), threshold1);

        var threshold2 = ThresholdConfig.builder()
                .id(2L)
                .columnComparisonConfig(column2)
                .thresholdValue(new BigDecimal("3.00")) // 3 units threshold
                .severity(Severity.MEDIUM)
                .notificationEnabled(true)
                .build();
        thresholdConfigs.put(column2.getId(), threshold2);
    }

    @Test
    @DisplayName("Should not exceed thresholds when differences are small")
    void testCompareNoThresholdExceeded() {
        // Prepare mock data
        List<String> sourceColumns = List.of("amount", "count");
        List<String> targetColumns = List.of("amount", "total_count");

        // Cross-table data
        List<Map<String, Object>> crossTableData = new ArrayList<>();

        // First row
        Map<String, Object> row1 = Map.of(
                "s_amount", new BigDecimal("100.00"),
                "t_amount", new BigDecimal("99.00"),
                "s_count", 10,
                "t_total_count", 9
        );
        crossTableData.add(row1);

        // Second row
        Map<String, Object> row2 = Map.of(
                "s_amount", new BigDecimal("200.00"),
                "t_amount", new BigDecimal("198.00"),
                "s_count", 20,
                "t_total_count", 19
        );
        crossTableData.add(row2);

        // Mock repository behavior
        when(dynamicTableRepository.executeCrossTableQuery(
                eq("source_table"), eq("target_table"),
                eq(sourceColumns), eq(targetColumns),
                eq("source_table.id = target_table.source_id"),
                eq("created_date"), isNull()))
                .thenReturn(crossTableData);

        // Execute comparison
        var results = crossTableComparator.compare(crossTableConfig, columnConfigs, thresholdConfigs);

        // Verify results
        assertEquals(4, results.size(), "Should have 4 results (2 columns x 2 rows)");

        // Verify no thresholds were exceeded
        assertFalse(results.stream().anyMatch(ValidationDetailResult::isThresholdExceeded),
                "No thresholds should be exceeded");

        // Verify amount comparison for first row
        var amountResult1 = results.stream()
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
        assertFalse(amountResult1.isThresholdExceeded(), "1.01% < 5% threshold");

        // Verify count comparison for first row
        var countResult1 = results.stream()
                .filter(r -> r.getColumnComparisonConfig().getColumnName().equals("count") &&
                        r.getActualValue().equals(new BigDecimal("10")))
                .findFirst()
                .orElseThrow();

        assertEquals(new BigDecimal("10"), countResult1.getActualValue());
        assertEquals(new BigDecimal("9"), countResult1.getExpectedValue());
        assertEquals(new BigDecimal("1"), countResult1.getDifferenceValue());
        assertFalse(countResult1.isThresholdExceeded(), "1 < 3 threshold");
    }

    @Test
    @DisplayName("Should exceed thresholds when differences are large")
    void testCompareThresholdExceeded() {
        // Prepare mock data
        List<String> sourceColumns = List.of("amount", "count");
        List<String> targetColumns = List.of("amount", "total_count");

        // Cross-table data with large differences
        List<Map<String, Object>> crossTableData = List.of(
                Map.of(
                        "s_amount", new BigDecimal("100.00"),
                        "t_amount", new BigDecimal("90.00"),
                        "s_count", 10,
                        "t_total_count", 6
                )
        );

        // Mock repository behavior
        when(dynamicTableRepository.executeCrossTableQuery(
                eq("source_table"), eq("target_table"),
                eq(sourceColumns), eq(targetColumns),
                eq("source_table.id = target_table.source_id"),
                eq("created_date"), isNull()))
                .thenReturn(crossTableData);

        // Execute comparison
        var results = crossTableComparator.compare(crossTableConfig, columnConfigs, thresholdConfigs);

        // Verify results
        assertEquals(2, results.size(), "Should have 2 results (2 columns x 1 row)");

        // Verify all thresholds were exceeded
        assertTrue(results.stream().allMatch(ValidationDetailResult::isThresholdExceeded),
                "All thresholds should be exceeded");

        // Verify amount comparison
        var amountResult = results.stream()
                .filter(r -> r.getColumnComparisonConfig().getColumnName().equals("amount"))
                .findFirst()
                .orElseThrow();

        assertEquals(new BigDecimal("100.00"), amountResult.getActualValue());
        assertEquals(new BigDecimal("90.00"), amountResult.getExpectedValue());
        assertEquals(new BigDecimal("10.00"), amountResult.getDifferenceValue());

        // Difference percentage should be about 11.11%
        assertTrue(amountResult.getDifferencePercentage().compareTo(new BigDecimal("11.1")) > 0);
        assertTrue(amountResult.getDifferencePercentage().compareTo(new BigDecimal("11.12")) < 0);
        assertTrue(amountResult.isThresholdExceeded(), "11.11% > 5% threshold");

        // Verify count comparison
        var countResult = results.stream()
                .filter(r -> r.getColumnComparisonConfig().getColumnName().equals("count"))
                .findFirst()
                .orElseThrow();

        assertEquals(new BigDecimal("10"), countResult.getActualValue());
        assertEquals(new BigDecimal("6"), countResult.getExpectedValue());
        assertEquals(new BigDecimal("4"), countResult.getDifferenceValue());
        assertTrue(countResult.isThresholdExceeded(), "4 > 3 threshold");
    }

    @Test
    @DisplayName("Should handle null values correctly")
    void testCompareWithNullValues() {
        // Prepare mock data
        List<String> sourceColumns = List.of("amount", "count");
        List<String> targetColumns = List.of("amount", "total_count");

        // Cross-table data with null values
        List<Map<String, Object>> crossTableData = List.of(
                Map.of(
                        "s_amount", null,
                        "t_amount", new BigDecimal("90.00"),
                        "s_count", 10,
                        "t_total_count", null
                )
        );

        // Mock repository behavior
        when(dynamicTableRepository.executeCrossTableQuery(
                any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(crossTableData);

        // Execute comparison
        var results = crossTableComparator.compare(crossTableConfig, columnConfigs, thresholdConfigs);

        // Our null handling strategy is TREAT_AS_ZERO, so we should still get results
        assertFalse(results.isEmpty(), "Should get results even with null values");

        // Verify amount comparison
        var amountResult = results.stream()
                .filter(r -> r.getColumnComparisonConfig().getColumnName().equals("amount"))
                .findFirst()
                .orElse(null);

        assertNotNull(amountResult, "Should have an amount result");
        assertEquals(BigDecimal.ZERO, amountResult.getActualValue());
        assertEquals(new BigDecimal("90.00"), amountResult.getExpectedValue());

        // Verify count comparison
        var countResult = results.stream()
                .filter(r -> r.getColumnComparisonConfig().getColumnName().equals("count"))
                .findFirst()
                .orElse(null);

        assertNotNull(countResult, "Should have a count result");
        assertEquals(new BigDecimal("10"), countResult.getActualValue());
        assertEquals(BigDecimal.ZERO, countResult.getExpectedValue());
    }
}
