package com.company.datavalidation.api;

import com.company.datavalidation.api.ConfigurationController;
import com.company.datavalidation.model.*;
import com.company.datavalidation.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class ConfigurationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ComparisonConfigRepository comparisonConfigRepository;

    @Mock
    private DayOverDayConfigRepository dayOverDayConfigRepository;

    @Mock
    private CrossTableConfigRepository crossTableConfigRepository;

    @Mock
    private ColumnComparisonConfigRepository columnComparisonConfigRepository;

    @Mock
    private ThresholdConfigRepository thresholdConfigRepository;

    @InjectMocks
    private ConfigurationController configurationController;

    private ObjectMapper objectMapper = new ObjectMapper();

    private ComparisonConfig comparisonConfig;
    private DayOverDayConfig dayOverDayConfig;
    private CrossTableConfig crossTableConfig;
    private ColumnComparisonConfig columnComparisonConfig;
    private ThresholdConfig thresholdConfig;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(configurationController).build();

        // Create test data
        comparisonConfig = new ComparisonConfig();
        comparisonConfig.setId(1L);
        comparisonConfig.setTableName("orders");
        comparisonConfig.setEnabled(true);
        comparisonConfig.setDescription("Orders table validation");
        comparisonConfig.setCreatedDate(LocalDateTime.now());
        comparisonConfig.setLastModifiedDate(LocalDateTime.now());

        dayOverDayConfig = new DayOverDayConfig();
        dayOverDayConfig.setId(1L);
        dayOverDayConfig.setComparisonConfig(comparisonConfig);
        dayOverDayConfig.setEnabled(true);
        dayOverDayConfig.setExclusionCondition("status <> 'CANCELED'");

        crossTableConfig = new CrossTableConfig();
        crossTableConfig.setId(1L);
        crossTableConfig.setSourceComparisonConfig(comparisonConfig);
        crossTableConfig.setTargetTableName("order_items");
        crossTableConfig.setJoinCondition("orders.id = order_items.order_id");
        crossTableConfig.setEnabled(true);

        columnComparisonConfig = new ColumnComparisonConfig();
        columnComparisonConfig.setId(1L);
        columnComparisonConfig.setDayOverDayConfig(dayOverDayConfig);
        columnComparisonConfig.setColumnName("total_amount");
        columnComparisonConfig.setComparisonType(ComparisonType.PERCENTAGE);
        columnComparisonConfig.setNullHandlingStrategy(HandlingStrategy.TREAT_AS_ZERO);
        columnComparisonConfig.setBlankHandlingStrategy(HandlingStrategy.TREAT_AS_ZERO);
        columnComparisonConfig.setNaHandlingStrategy(HandlingStrategy.TREAT_AS_ZERO);

        thresholdConfig = new ThresholdConfig();
        thresholdConfig.setId(1L);
        thresholdConfig.setColumnComparisonConfig(columnComparisonConfig);
        thresholdConfig.setThresholdValue(new BigDecimal("10.00"));
        thresholdConfig.setSeverity(Severity.HIGH);
        thresholdConfig.setNotificationEnabled(true);
    }

    @Test
    public void testGetAllConfigs() throws Exception {
        when(comparisonConfigRepository.findAll()).thenReturn(Arrays.asList(comparisonConfig));

        mockMvc.perform(get("/api/v1/configs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].tableName", is("orders")));

        verify(comparisonConfigRepository).findAll();
    }

    @Test
    public void testGetConfigById() throws Exception {
        when(comparisonConfigRepository.findById(1L)).thenReturn(Optional.of(comparisonConfig));

        mockMvc.perform(get("/api/v1/configs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.tableName", is("orders")));

        verify(comparisonConfigRepository).findById(1L);
    }

    @Test
    public void testGetConfigById_NotFound() throws Exception {
        when(comparisonConfigRepository.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/configs/1"))
                .andExpect(status().isNotFound());

        verify(comparisonConfigRepository).findById(1L);
    }

    @Test
    public void testCreateConfig() throws Exception {
        when(comparisonConfigRepository.save(any(ComparisonConfig.class))).thenReturn(comparisonConfig);

        mockMvc.perform(post("/api/v1/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(comparisonConfig)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.tableName", is("orders")));

        verify(comparisonConfigRepository).save(any(ComparisonConfig.class));
    }

    @Test
    public void testUpdateConfig() throws Exception {
        when(comparisonConfigRepository.findById(1L)).thenReturn(Optional.of(comparisonConfig));
        when(comparisonConfigRepository.save(any(ComparisonConfig.class))).thenReturn(comparisonConfig);

        mockMvc.perform(put("/api/v1/configs/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(comparisonConfig)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.tableName", is("orders")));

        verify(comparisonConfigRepository).findById(1L);
        verify(comparisonConfigRepository).save(any(ComparisonConfig.class));
    }

    @Test
    public void testUpdateConfig_NotFound() throws Exception {
        when(comparisonConfigRepository.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/v1/configs/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(comparisonConfig)))
                .andExpect(status().isNotFound());

        verify(comparisonConfigRepository).findById(1L);
        verify(comparisonConfigRepository, never()).save(any(ComparisonConfig.class));
    }

    @Test
    public void testDeleteConfig() throws Exception {
        when(comparisonConfigRepository.findById(1L)).thenReturn(Optional.of(comparisonConfig));
        doNothing().when(comparisonConfigRepository).deleteById(1L);

        mockMvc.perform(delete("/api/v1/configs/1"))
                .andExpect(status().isNoContent());

        verify(comparisonConfigRepository).findById(1L);
        verify(comparisonConfigRepository).deleteById(1L);
    }

    @Test
    public void testDeleteConfig_NotFound() throws Exception {
        when(comparisonConfigRepository.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/v1/configs/1"))
                .andExpect(status().isNotFound());

        verify(comparisonConfigRepository).findById(1L);
        verify(comparisonConfigRepository, never()).deleteById(anyLong());
    }

    @Test
    public void testGetDayOverDayConfig() throws Exception {
        when(dayOverDayConfigRepository.findByComparisonConfigId(1L)).thenReturn(Optional.of(dayOverDayConfig));

        mockMvc.perform(get("/api/v1/configs/1/day-over-day"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));

        verify(dayOverDayConfigRepository).findByComparisonConfigId(1L);
    }

    @Test
    public void testCreateDayOverDayConfig() throws Exception {
        when(comparisonConfigRepository.findById(1L)).thenReturn(Optional.of(comparisonConfig));
        when(dayOverDayConfigRepository.save(any(DayOverDayConfig.class))).thenReturn(dayOverDayConfig);

        mockMvc.perform(post("/api/v1/configs/1/day-over-day")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dayOverDayConfig)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)));

        verify(comparisonConfigRepository).findById(1L);
        verify(dayOverDayConfigRepository).save(any(DayOverDayConfig.class));
    }

    @Test
    public void testGetCrossTableConfigs() throws Exception {
        when(comparisonConfigRepository.findById(1L)).thenReturn(Optional.of(comparisonConfig));
        when(crossTableConfigRepository.findBySourceComparisonConfigAndEnabled(eq(comparisonConfig), eq(true)))
                .thenReturn(Collections.singletonList(crossTableConfig));

        mockMvc.perform(get("/api/v1/configs/1/cross-table"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].targetTableName", is("order_items")));

        verify(comparisonConfigRepository).findById(1L);
        verify(crossTableConfigRepository).findBySourceComparisonConfigAndEnabled(eq(comparisonConfig), eq(true));
    }

    @Test
    public void testCreateCrossTableConfig() throws Exception {
        when(comparisonConfigRepository.findById(1L)).thenReturn(Optional.of(comparisonConfig));
        when(crossTableConfigRepository.save(any(CrossTableConfig.class))).thenReturn(crossTableConfig);

        mockMvc.perform(post("/api/v1/configs/1/cross-table")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crossTableConfig)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.targetTableName", is("order_items")));

        verify(comparisonConfigRepository).findById(1L);
        verify(crossTableConfigRepository).save(any(CrossTableConfig.class));
    }

    @Test
    public void testGetColumnConfigs() throws Exception {
        when(columnComparisonConfigRepository.findByDayOverDayConfigId(1L))
                .thenReturn(Collections.singletonList(columnComparisonConfig));

        mockMvc.perform(get("/api/v1/configs/day-over-day/1/columns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].columnName", is("total_amount")));

        verify(columnComparisonConfigRepository).findByDayOverDayConfigId(1L);
    }

    @Test
    public void testCreateColumnConfig() throws Exception {
        when(dayOverDayConfigRepository.findById(1L)).thenReturn(Optional.of(dayOverDayConfig));
        when(columnComparisonConfigRepository.save(any(ColumnComparisonConfig.class))).thenReturn(columnComparisonConfig);

        mockMvc.perform(post("/api/v1/configs/day-over-day/1/columns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(columnComparisonConfig)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.columnName", is("total_amount")));

        verify(dayOverDayConfigRepository).findById(1L);
        verify(columnComparisonConfigRepository).save(any(ColumnComparisonConfig.class));
    }

    @Test
    public void testGetThresholdConfigs() throws Exception {
        when(thresholdConfigRepository.findByColumnComparisonConfigId(1L))
                .thenReturn(Collections.singletonList(thresholdConfig));

        mockMvc.perform(get("/api/v1/configs/columns/1/thresholds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].severity", is("HIGH")));

        verify(thresholdConfigRepository).findByColumnComparisonConfigId(1L);
    }

    @Test
    public void testCreateThresholdConfig() throws Exception {
        when(columnComparisonConfigRepository.findById(1L)).thenReturn(Optional.of(columnComparisonConfig));
        when(thresholdConfigRepository.save(any(ThresholdConfig.class))).thenReturn(thresholdConfig);

        mockMvc.perform(post("/api/v1/configs/columns/1/thresholds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(thresholdConfig)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.severity", is("HIGH")));

        verify(columnComparisonConfigRepository).findById(1L);
        verify(thresholdConfigRepository).save(any(ThresholdConfig.class));
    }
}
