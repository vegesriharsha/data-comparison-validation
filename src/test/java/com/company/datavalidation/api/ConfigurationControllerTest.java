package com.company.datavalidation.api;

import com.company.datavalidation.model.*;
import com.company.datavalidation.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Configuration Controller Tests")
class ConfigurationControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

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

    // Test data
    private ComparisonConfig comparisonConfig;
    private DayOverDayConfig dayOverDayConfig;
    private CrossTableConfig crossTableConfig;
    private ColumnComparisonConfig columnComparisonConfig;
    private ThresholdConfig thresholdConfig;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(configurationController).build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // For LocalDateTime serialization

        // Create test data using Lombok builders
        comparisonConfig = ComparisonConfig.builder()
                .id(1L)
                .tableName("orders")
                .enabled(true)
                .description("Orders table validation")
                .createdDate(LocalDateTime.now())
                .lastModifiedDate(LocalDateTime.now())
                .build();

        dayOverDayConfig = DayOverDayConfig.builder()
                .id(1L)
                .comparisonConfig(comparisonConfig)
                .enabled(true)
                .exclusionCondition("status <> 'CANCELED'")
                .build();

        crossTableConfig = CrossTableConfig.builder()
                .id(1L)
                .sourceComparisonConfig(comparisonConfig)
                .targetTableName("order_items")
                .joinCondition("orders.id = order_items.order_id")
                .enabled(true)
                .build();

        columnComparisonConfig = ColumnComparisonConfig.builder()
                .id(1L)
                .dayOverDayConfig(dayOverDayConfig)
                .columnName("total_amount")
                .comparisonType(ComparisonType.PERCENTAGE)
                .nullHandlingStrategy(HandlingStrategy.TREAT_AS_ZERO)
                .blankHandlingStrategy(HandlingStrategy.TREAT_AS_ZERO)
                .naHandlingStrategy(HandlingStrategy.TREAT_AS_ZERO)
                .build();

        thresholdConfig = ThresholdConfig.builder()
                .id(1L)
                .columnComparisonConfig(columnComparisonConfig)
                .thresholdValue(new BigDecimal("10.00"))
                .severity(Severity.HIGH)
                .notificationEnabled(true)
                .build();
    }

    @Test
    @DisplayName("Should get all configs")
    void testGetAllConfigs() throws Exception {
        when(comparisonConfigRepository.findAll()).thenReturn(List.of(comparisonConfig));

        mockMvc.perform(get("/api/v1/configs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].tableName", is("orders")));

        verify(comparisonConfigRepository).findAll();
    }

    @Test
    @DisplayName("Should get config by ID")
    void testGetConfigById() throws Exception {
        when(comparisonConfigRepository.findById(1L)).thenReturn(Optional.of(comparisonConfig));

        mockMvc.perform(get("/api/v1/configs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.tableName", is("orders")));

        verify(comparisonConfigRepository).findById(1L);
    }

    @Test
    @DisplayName("Should return not found when config doesn't exist")
    void testGetConfigByIdNotFound() throws Exception {
        when(comparisonConfigRepository.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/configs/1"))
                .andExpect(status().isNotFound());

        verify(comparisonConfigRepository).findById(1L);
    }

    @Test
    @DisplayName("Should create config")
    void testCreateConfig() throws Exception {
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
    @DisplayName("Should update config")
    void testUpdateConfig() throws Exception {
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
    @DisplayName("Should return not found when updating non-existent config")
    void testUpdateConfigNotFound() throws Exception {
        when(comparisonConfigRepository.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/v1/configs/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(comparisonConfig)))
                .andExpect(status().isNotFound());

        verify(comparisonConfigRepository).findById(1L);
        verify(comparisonConfigRepository, never()).save(any(ComparisonConfig.class));
    }

    @Test
    @DisplayName("Should delete config")
    void testDeleteConfig() throws Exception {
        when(comparisonConfigRepository.findById(1L)).thenReturn(Optional.of(comparisonConfig));
        doNothing().when(comparisonConfigRepository).deleteById(1L);

        mockMvc.perform(delete("/api/v1/configs/1"))
                .andExpect(status().isNoContent());

        verify(comparisonConfigRepository).findById(1L);
        verify(comparisonConfigRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Should return not found when deleting non-existent config")
    void testDeleteConfigNotFound() throws Exception {
        when(comparisonConfigRepository.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/v1/configs/1"))
                .andExpect(status().isNotFound());

        verify(comparisonConfigRepository).findById(1L);
        verify(comparisonConfigRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("Should get day-over-day config")
    void testGetDayOverDayConfig() throws Exception {
        when(dayOverDayConfigRepository.findByComparisonConfigId(1L)).thenReturn(Optional.of(dayOverDayConfig));

        mockMvc.perform(get("/api/v1/configs/1/day-over-day"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));

        verify(dayOverDayConfigRepository).findByComparisonConfigId(1L);
    }

    @Test
    @DisplayName("Should create day-over-day config")
    void testCreateDayOverDayConfig() throws Exception {
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
    @DisplayName("Should get cross-table configs")
    void testGetCrossTableConfigs() throws Exception {
        when(comparisonConfigRepository.findById(1L)).thenReturn(Optional.of(comparisonConfig));
        when(crossTableConfigRepository.findBySourceComparisonConfigAndEnabled(eq(comparisonConfig), eq(true)))
                .thenReturn(List.of(crossTableConfig));

        mockMvc.perform(get("/api/v1/configs/1/cross-table"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].targetTableName", is("order_items")));

        verify(comparisonConfigRepository).findById(1L);
        verify(crossTableConfigRepository).findBySourceComparisonConfigAndEnabled(eq(comparisonConfig), eq(true));
    }

    @Test
    @DisplayName("Should create cross-table config")
    void testCreateCrossTableConfig() throws Exception {
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
    @DisplayName("Should get column configs for day-over-day")
    void testGetColumnConfigsForDayOverDay() throws Exception {
        when(columnComparisonConfigRepository.findByDayOverDayConfigId(1L))
                .thenReturn(List.of(columnComparisonConfig));

        mockMvc.perform(get("/api/v1/configs/day-over-day/1/columns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].columnName", is("total_amount")));

        verify(columnComparisonConfigRepository).findByDayOverDayConfigId(1L);
    }

    @Test
    @DisplayName("Should create column config for day-over-day")
    void testCreateColumnConfigForDayOverDay() throws Exception {
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
    @DisplayName("Should get column configs for cross-table")
    void testGetColumnConfigsForCrossTable() throws Exception {
        // Update column config to use cross-table
        ColumnComparisonConfig crossTableColumnConfig = ColumnComparisonConfig.builder()
                .id(2L)
                .crossTableConfig(crossTableConfig)
                .columnName("product_count")
                .targetColumnName("item_count")
                .comparisonType(ComparisonType.ABSOLUTE)
                .nullHandlingStrategy(HandlingStrategy.TREAT_AS_ZERO)
                .blankHandlingStrategy(HandlingStrategy.TREAT_AS_ZERO)
                .naHandlingStrategy(HandlingStrategy.TREAT_AS_ZERO)
                .build();

        when(columnComparisonConfigRepository.findByCrossTableConfigId(1L))
                .thenReturn(List.of(crossTableColumnConfig));

        mockMvc.perform(get("/api/v1/configs/cross-table/1/columns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(2)))
                .andExpect(jsonPath("$[0].columnName", is("product_count")));

        verify(columnComparisonConfigRepository).findByCrossTableConfigId(1L);
    }

    @Test
    @DisplayName("Should create column config for cross-table")
    void testCreateColumnConfigForCrossTable() throws Exception {
        // Update column config to use cross-table
        ColumnComparisonConfig crossTableColumnConfig = ColumnComparisonConfig.builder()
                .id(2L)
                .crossTableConfig(crossTableConfig)
                .columnName("product_count")
                .targetColumnName("item_count")
                .comparisonType(ComparisonType.ABSOLUTE)
                .nullHandlingStrategy(HandlingStrategy.TREAT_AS_ZERO)
                .blankHandlingStrategy(HandlingStrategy.TREAT_AS_ZERO)
                .naHandlingStrategy(HandlingStrategy.TREAT_AS_ZERO)
                .build();

        when(crossTableConfigRepository.findById(1L)).thenReturn(Optional.of(crossTableConfig));
        when(columnComparisonConfigRepository.save(any(ColumnComparisonConfig.class))).thenReturn(crossTableColumnConfig);

        mockMvc.perform(post("/api/v1/configs/cross-table/1/columns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crossTableColumnConfig)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(2)))
                .andExpect(jsonPath("$.columnName", is("product_count")));

        verify(crossTableConfigRepository).findById(1L);
        verify(columnComparisonConfigRepository).save(any(ColumnComparisonConfig.class));
    }

    @Test
    @DisplayName("Should get threshold configs")
    void testGetThresholdConfigs() throws Exception {
        when(thresholdConfigRepository.findByColumnComparisonConfigId(1L))
                .thenReturn(List.of(thresholdConfig));

        mockMvc.perform(get("/api/v1/configs/columns/1/thresholds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].severity", is("HIGH")));

        verify(thresholdConfigRepository).findByColumnComparisonConfigId(1L);
    }

    @Test
    @DisplayName("Should create threshold config")
    void testCreateThresholdConfig() throws Exception {
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

    @Test
    @DisplayName("Should return not found when creating threshold for non-existent column")
    void testCreateThresholdConfigColumnNotFound() throws Exception {
        when(columnComparisonConfigRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/configs/columns/999/thresholds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(thresholdConfig)))
                .andExpect(status().isNotFound());

        verify(columnComparisonConfigRepository).findById(999L);
        verify(thresholdConfigRepository, never()).save(any(ThresholdConfig.class));
    }

    @Test
    @DisplayName("Should return bad request for invalid config type")
    void testGetColumnConfigsInvalidType() throws Exception {
        mockMvc.perform(get("/api/v1/configs/invalid-type/1/columns"))
                .andExpect(status().isBadRequest());
    }
}
