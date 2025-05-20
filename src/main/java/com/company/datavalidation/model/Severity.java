package com.company.datavalidation.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

/**
 * Severity levels for threshold violations
 */
@Getter
@Slf4j
@RequiredArgsConstructor
@ToString
public enum Severity {

    HIGH(3, "Critical threshold violation"),
    MEDIUM(2, "Warning threshold violation"),
    LOW(1, "Information threshold violation");

    private final int level;

    private final String description;

    // Constructor is handled by @RequiredArgsConstructor

    /**
     * Determine if this severity is higher than another
     *
     * @param other The other severity to compare
     * @return True if this severity is higher than the other
     */
    public boolean isHigherThan(Severity other) {
        return this.level > other.level;
    }

    /**
     * Get the highest severity from a collection of severities
     *
     * @param severities The severities to compare
     * @return The highest severity, or LOW if none are provided
     */
    public static Severity getHighest(Severity... severities) {
        if (severities == null || severities.length == 0) {
            log.debug("No severities provided, returning LOW as default");
            return LOW;
        }

        Optional<Severity> highest = Arrays.stream(severities)
                .filter(Objects::nonNull)
                .max(Comparator.comparingInt(Severity::getLevel));

        return highest.orElse(LOW);
    }

    /**
     * Parse a severity level from string, with fallback to default
     *
     * @param name The severity name
     * @param defaultSeverity The default severity if not found
     * @return The parsed severity
     */
    public static Severity fromName(String name, Severity defaultSeverity) {
        if (name == null || name.isBlank()) {
            return defaultSeverity;
        }

        try {
            return Severity.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown severity level: {}, using default: {}", name, defaultSeverity);
            return defaultSeverity;
        }
    }

    /**
     * Get severity color code for UI display
     *
     * @return The color code
     */
    public String getColorCode() {
        return switch (this) {
            case HIGH -> "#ff0000"; // Red
            case MEDIUM -> "#ffa500"; // Orange
            case LOW -> "#ffff00"; // Yellow
        };
    }
}
