package com.company.datavalidation.api;

import com.company.datavalidation.model.*;
import com.company.datavalidation.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/configs")
@Tag(name = "Configuration Management", description = "APIs for managing validation configurations")
public class ConfigurationController {

    private final ComparisonConfigRepository comparisonConfigRepository;
    private final DayOverDayConfigRepository dayOverDayConfigRepository;
    private final CrossTableConfigRepository crossTableConfigRepository;
    private final ColumnComparisonConfigRepository columnComparisonConfigRepository;
    private final ThresholdConfigRepository thresholdConfigRepository;

    @Autowired
    public ConfigurationController(
            ComparisonConfigRepository comparisonConfigRepository,
            DayOverDayConfigRepository dayOverDayConfigRepository,
            CrossTableConfigRepository crossTableConfigRepository,
            ColumnComparisonConfigRepository columnComparisonConfigRepository,
            ThresholdConfigRepository thresholdConfigRepository) {
        this.comparisonConfigRepository = comparisonConfigRepository;
        this.dayOverDayConfigRepository = dayOverDayConfigRepository;
        this.crossTableConfigRepository = crossTableConfigRepository;
        this.columnComparisonConfigRepository = columnComparisonConfigRepository;
        this.thresholdConfigRepository = thresholdConfigRepository;
    }

    @GetMapping
    @Operation(summary = "List all comparison configurations")
    public ResponseEntity<List<ComparisonConfig>> getAllConfigs() {
        List<ComparisonConfig> configs = comparisonConfigRepository.findAll();
        return ResponseEntity.ok(configs);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get specific configuration by ID")
    public ResponseEntity<ComparisonConfig> getConfigById(@PathVariable Long id) {
        Optional<ComparisonConfig> config = comparisonConfigRepository.findById(id);
        return config.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create new configuration")
    public ResponseEntity<ComparisonConfig> createConfig(@RequestBody ComparisonConfig config) {
        // Set created date and modifier
        config.setCreatedDate(LocalDateTime.now());
        config.setLastModifiedDate(LocalDateTime.now());
        config.setCreatedBy("api-user"); // This should come from authentication
        config.setLastModifiedBy("api-user"); // This should come from authentication

        ComparisonConfig savedConfig = comparisonConfigRepository.save(config);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedConfig);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update configuration")
    public ResponseEntity<ComparisonConfig> updateConfig(@PathVariable Long id, @RequestBody ComparisonConfig config) {
        Optional<ComparisonConfig> existingConfig = comparisonConfigRepository.findById(id);

        if (existingConfig.isPresent()) {
            ComparisonConfig configToUpdate = existingConfig.get();

            // Update fields
            configToUpdate.setTableName(config.getTableName());
            configToUpdate.setEnabled(config.isEnabled());
            configToUpdate.setDescription(config.getDescription());
            configToUpdate.setLastModifiedDate(LocalDateTime.now());
            configToUpdate.setLastModifiedBy("api-user"); // This should come from authentication

            ComparisonConfig updatedConfig = comparisonConfigRepository.save(configToUpdate);
            return ResponseEntity.ok(updatedConfig);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete configuration")
    public ResponseEntity<Void> deleteConfig(@PathVariable Long id) {
        Optional<ComparisonConfig> existingConfig = comparisonConfigRepository.findById(id);

        if (existingConfig.isPresent()) {
            comparisonConfigRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/day-over-day")
    @Operation(summary = "Get day-over-day configuration for a comparison config")
    public ResponseEntity<DayOverDayConfig> getDayOverDayConfig(@PathVariable Long id) {
        Optional<DayOverDayConfig> config = dayOverDayConfigRepository.findByComparisonConfigId(id);
        return config.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/day-over-day")
    @Operation(summary = "Create day-over-day configuration for a comparison config")
    public ResponseEntity<DayOverDayConfig> createDayOverDayConfig(
            @PathVariable Long id, @RequestBody DayOverDayConfig config) {

        Optional<ComparisonConfig> existingConfig = comparisonConfigRepository.findById(id);

        if (existingConfig.isPresent()) {
            config.setComparisonConfig(existingConfig.get());
            DayOverDayConfig savedConfig = dayOverDayConfigRepository.save(config);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedConfig);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/cross-table")
    @Operation(summary = "Get cross-table configurations for a comparison config")
    public ResponseEntity<List<CrossTableConfig>> getCrossTableConfigs(@PathVariable Long id) {
        Optional<ComparisonConfig> existingConfig = comparisonConfigRepository.findById(id);

        if (existingConfig.isPresent()) {
            List<CrossTableConfig> configs = crossTableConfigRepository.findBySourceComparisonConfigAndEnabled(
                    existingConfig.get(), true);
            return ResponseEntity.ok(configs);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/cross-table")
    @Operation(summary = "Create cross-table configuration for a comparison config")
    public ResponseEntity<CrossTableConfig> createCrossTableConfig(
            @PathVariable Long id, @RequestBody CrossTableConfig config) {

        Optional<ComparisonConfig> existingConfig = comparisonConfigRepository.findById(id);

        if (existingConfig.isPresent()) {
            config.setSourceComparisonConfig(existingConfig.get());
            CrossTableConfig savedConfig = crossTableConfigRepository.save(config);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedConfig);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{configType}/{configId}/columns")
    @Operation(summary = "Get column configurations for a day-over-day or cross-table config")
    public ResponseEntity<List<ColumnComparisonConfig>> getColumnConfigs(
            @PathVariable String configType, @PathVariable Long configId) {

        List<ColumnComparisonConfig> configs;

        if ("day-over-day".equals(configType)) {
            configs = columnComparisonConfigRepository.findByDayOverDayConfigId(configId);
        } else if ("cross-table".equals(configType)) {
            configs = columnComparisonConfigRepository.findByCrossTableConfigId(configId);
        } else {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(configs);
    }

    @PostMapping("/{configType}/{configId}/columns")
    @Operation(summary = "Create column configuration for a day-over-day or cross-table config")
    public ResponseEntity<ColumnComparisonConfig> createColumnConfig(
            @PathVariable String configType, @PathVariable Long configId,
            @RequestBody ColumnComparisonConfig config) {

        if ("day-over-day".equals(configType)) {
            Optional<DayOverDayConfig> existingConfig = dayOverDayConfigRepository.findById(configId);

            if (existingConfig.isPresent()) {
                config.setDayOverDayConfig(existingConfig.get());
                config.setCrossTableConfig(null);
                ColumnComparisonConfig savedConfig = columnComparisonConfigRepository.save(config);
                return ResponseEntity.status(HttpStatus.CREATED).body(savedConfig);
            }
        } else if ("cross-table".equals(configType)) {
            Optional<CrossTableConfig> existingConfig = crossTableConfigRepository.findById(configId);

            if (existingConfig.isPresent()) {
                config.setCrossTableConfig(existingConfig.get());
                config.setDayOverDayConfig(null);
                ColumnComparisonConfig savedConfig = columnComparisonConfigRepository.save(config);
                return ResponseEntity.status(HttpStatus.CREATED).body(savedConfig);
            }
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/columns/{columnId}/thresholds")
    @Operation(summary = "Get threshold configurations for a column config")
    public ResponseEntity<List<ThresholdConfig>> getThresholdConfigs(@PathVariable Long columnId) {
        List<ThresholdConfig> configs = thresholdConfigRepository.findByColumnComparisonConfigId(columnId);
        return ResponseEntity.ok(configs);
    }

    @PostMapping("/columns/{columnId}/thresholds")
    @Operation(summary = "Create threshold configuration for a column config")
    public ResponseEntity<ThresholdConfig> createThresholdConfig(
            @PathVariable Long columnId, @RequestBody ThresholdConfig config) {

        Optional<ColumnComparisonConfig> existingConfig = columnComparisonConfigRepository.findById(columnId);

        if (existingConfig.isPresent()) {
            config.setColumnComparisonConfig(existingConfig.get());
            ThresholdConfig savedConfig = thresholdConfigRepository.save(config);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedConfig);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
