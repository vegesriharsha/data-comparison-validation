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
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DayOverDayComparatorTest {

    @Mock
    private DynamicTableRepository dynamicTableRepository;

    @InjectMocks
    private DayOverDayComparator dayOverDayComparator;

    private ComparisonConfig comparisonConfig;
    private DayOverDayConfig dayOverDayConfig;
    private List<ColumnComparisonConfig> columnConfigs;
    private Map<Long, ThresholdConfig> thresholdConfigs;

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
        dayOverDayConfig.setExclusionCondition("status <> 'CANCELED'");

        // Setup column configs
        columnConfigs = new ArrayList<>();

        ColumnComparisonConfig column1 = new ColumnComparisonConfig();
        column1.setId(1L);
        column1.setDayOverDayConfig(dayOverDayConfig);
        column1.setColumnName("amount");
        column1.setComparisonType(ComparisonType.PERCENTAGE);
        column1.setNullHandlingStrategy(HandlingStrategy.TREAT_AS_ZERO);
        column1.setBlankHandlingStrategy(HandlingStrategy.TREAT_AS_ZERO);
        column1.setNaHandlingStrategy(HandlingStrategy.TREAT_AS_ZERO);
        columnConfigs.add(column1);

        ColumnComparisonConfig column2 = new ColumnComparisonConfig();
        column2.setId(2L);
        column2.setDayOverDayConfig(dayOverDayConfig);
        column2.setColumnName("count");
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
        threshold1.setThresholdValue(new BigDecimal("10.00")); // 10% threshold
        threshold1.setSeverity(Severity.HIGH);
        threshold1.setNotificationEnabled(true);
        thresholdConfigs.put(column1.getId(), threshold1);

        ThresholdConfig threshold2 = new ThresholdConfig();
        threshold2.setId(2L);
        threshold2.setColumnComparisonConfig(column2);
        threshold2.setThresholdValue(new BigDecimal("5.00")); // 5 units threshold
        threshold2.setSeverity(Severity.MEDIUM);
        threshold2.setNotificationEnabled(true);
        thresholdConfigs.put(column2.getId(), threshold2);
    }

    @Test
    public void testCompare_NoThresholdExceeded() {
        // Prepare mock data
        List<String> columnNames = Arrays.asList("amount", "count");

        // Today's data
        List<Map<String, Object>> todayData = new ArrayList<>();
        Map<String, Object> todayRow = new HashMap<>();
        todayRow.put("amount", new BigDecimal("100.00"));
        todayRow.put("count", 20);
        todayData.add(todayRow);

        // Yesterday's data
        List<Map<String, Object>> yesterdayData = new ArrayList<>();
        Map<String, Object> yesterdayRow = new HashMap<>();
        yesterdayRow.put("amount", new BigDecimal("95.00"));
        yesterdayRow.put("count", 18);
        yesterdayData.add(yesterdayRow);

        // Mock repository behavior
        when(dynamicTableRepository.getDataForDate(
                eq("test_table"), eq(columnNames), eq("created_date"), any(LocalDate.class), eq("status <> 'CANCELED'")))
                .thenReturn(todayData, yesterdayData);

        // Execute comparison
        List<ValidationDetailResult> results = dayOverDayComparator.compare(dayOverDayConfig, columnConfigs, thresholdConfigs);

        // Verify results
        assertEquals(2, results.size());

        // Verify amount comparison - Use BigDecimal comparison
        ValidationDetailResult amountResult = results.stream()
                .filter(r -> r.getColumnComparisonConfig().getColumnName().equals("amount"))
                .findFirst()
                .orElseThrow();

        assertEquals(0, new BigDecimal("100.00").compareTo(amountResult.getActualValue()));
        assertEquals(0, new BigDecimal("95.00").compareTo(amountResult.getExpectedValue()));
        assertEquals(0, new BigDecimal("5.00").compareTo(amountResult.getDifferenceValue()));
        // Difference percentage should be about 5.26%
        assertTrue(amountResult.getDifferencePercentage().compareTo(new BigDecimal("5.2")) > 0);
        assertTrue(amountResult.getDifferencePercentage().compareTo(new BigDecimal("5.3")) < 0);
        assertFalse(amountResult.isThresholdExceeded()); // 5.26% < 10% threshold

        // Verify count comparison
        ValidationDetailResult countResult = results.stream()
                .filter(r -> r.getColumnComparisonConfig().getColumnName().equals("count"))
                .findFirst()
                .orElseThrow();

        assertEquals(0, new BigDecimal("20").compareTo(countResult.getActualValue()));
        assertEquals(0, new BigDecimal("18").compareTo(countResult.getExpectedValue()));
        assertEquals(0, new BigDecimal("2").compareTo(countResult.getDifferenceValue()));
        // Difference should be 2 units
        assertFalse(countResult.isThresholdExceeded()); // 2 < 5 threshold
    }

    @Test
    public void testCompare_ThresholdExceeded() {
        // Prepare mock data
        List<String> columnNames = Arrays.asList("amount", "count");

        // Today's data
        List<Map<String, Object>> todayData = new ArrayList<>();
        Map<String, Object> todayRow = new HashMap<>();
        todayRow.put("amount", new BigDecimal("120.00"));
        todayRow.put("count", 25);
        todayData.add(todayRow);

        // Yesterday's data
        List<Map<String, Object>> yesterdayData = new ArrayList<>();
        Map<String, Object> yesterdayRow = new HashMap<>();
        yesterdayRow.put("amount", new BigDecimal("95.00"));
        yesterdayRow.put("count", 18);
        yesterdayData.add(yesterdayRow);

        // Mock repository behavior
        when(dynamicTableRepository.getDataForDate(
                eq("test_table"), eq(columnNames), eq("created_date"), any(LocalDate.class), eq("status <> 'CANCELED'")))
                .thenReturn(todayData, yesterdayData);

        // Execute comparison
        List<ValidationDetailResult> results = dayOverDayComparator.compare(dayOverDayConfig, columnConfigs, thresholdConfigs);

        // Verify results
        assertEquals(2, results.size());

        // Verify amount comparison - Use BigDecimal comparison
        ValidationDetailResult amountResult = results.stream()
                .filter(r -> r.getColumnComparisonConfig().getColumnName().equals("amount"))
                .findFirst()
                .orElseThrow();

        assertEquals(0, new BigDecimal("120.00").compareTo(amountResult.getActualValue()));
        assertEquals(0, new BigDecimal("95.00").compareTo(amountResult.getExpectedValue()));
        assertEquals(0, new BigDecimal("25.00").compareTo(amountResult.getDifferenceValue()));
        // Difference percentage should be about 26.32%
        assertTrue(amountResult.getDifferencePercentage().compareTo(new BigDecimal("26.3")) > 0);
        assertTrue(amountResult.getDifferencePercentage().compareTo(new BigDecimal("26.4")) < 0);
        assertTrue(amountResult.isThresholdExceeded()); // 26.32% > 10% threshold

        // Verify count comparison
        ValidationDetailResult countResult = results.stream()
                .filter(r -> r.getColumnComparisonConfig().getColumnName().equals("count"))
                .findFirst()
                .orElseThrow();

        assertEquals(0, new BigDecimal("25").compareTo(countResult.getActualValue()));
        assertEquals(0, new BigDecimal("18").compareTo(countResult.getExpectedValue()));
        assertEquals(0, new BigDecimal("7").compareTo(countResult.getDifferenceValue()));
        // Difference should be 7 units
        assertTrue(countResult.isThresholdExceeded()); // 7 > 5 threshold
    }

    @Test
    public void testCompare_NullHandling() {
        // Prepare mock data
        List<String> columnNames = Arrays.asList("amount", "count");

        // Today's data
        List<Map<String, Object>> todayData = new ArrayList<>();
        Map<String, Object> todayRow = new HashMap<>();
        todayRow.put("amount", null);
        todayRow.put("count", "N/A");
        todayData.add(todayRow);

        // Yesterday's data
        List<Map<String, Object>> yesterdayData = new ArrayList<>();
        Map<String, Object> yesterdayRow = new HashMap<>();
        yesterdayRow.put("amount", new BigDecimal("95.00"));
        yesterdayRow.put("count", 18);
        yesterdayData.add(yesterdayRow);

        // Mock repository behavior
        when(dynamicTableRepository.getDataForDate(
                eq("test_table"), eq(columnNames), eq("created_date"), any(LocalDate.class), eq("status <> 'CANCELED'")))
                .thenReturn(todayData, yesterdayData);

        // Execute comparison
        List<ValidationDetailResult> results = dayOverDayComparator.compare(dayOverDayConfig, columnConfigs, thresholdConfigs);

        // Verify results
        assertEquals(2, results.size());

        // Verify amount comparison - null should be treated as zero - Use BigDecimal comparison
        ValidationDetailResult amountResult = results.stream()
                .filter(r -> r.getColumnComparisonConfig().getColumnName().equals("amount"))
                .findFirst()
                .orElseThrow();

        assertEquals(0, new BigDecimal("0.00").compareTo(amountResult.getActualValue()));
        assertEquals(0, new BigDecimal("95.00").compareTo(amountResult.getExpectedValue()));
        assertEquals(0, new BigDecimal("-95.00").compareTo(amountResult.getDifferenceValue()));
        // Difference percentage should be -100%
        assertEquals(0, new BigDecimal("-100").compareTo(amountResult.getDifferencePercentage()));
        assertTrue(amountResult.isThresholdExceeded()); // -100% > 10% threshold

        // Verify count comparison - N/A should be treated as zero
        ValidationDetailResult countResult = results.stream()
                .filter(r -> r.getColumnComparisonConfig().getColumnName().equals("count"))
                .findFirst()
                .orElseThrow();

        assertEquals(0, new BigDecimal("0").compareTo(countResult.getActualValue()));
        assertEquals(0, new BigDecimal("18").compareTo(countResult.getExpectedValue()));
        assertEquals(0, new BigDecimal("-18").compareTo(countResult.getDifferenceValue()));
        // Difference should be -18 units
        assertTrue(countResult.isThresholdExceeded()); // -18 > 5 threshold (absolute)
    }
}
