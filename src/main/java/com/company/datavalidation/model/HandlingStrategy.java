package com.company.datavalidation.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.function.Function;

/**
 * Strategies for handling special values (null, blank, N/A)
 */
@Getter
@Slf4j
@RequiredArgsConstructor
@ToString
public enum HandlingStrategy {

    IGNORE(
            "Ignore special values",
            value -> null
    ),

    TREAT_AS_NULL(
            "Treat special values as null",
            value -> null
    ),

    TREAT_AS_ZERO(
            "Treat special values as zero",
            value -> BigDecimal.ZERO
    ),

    FAIL(
            "Fail on special values",
            value -> {
                throw new RuntimeException("Special value encountered with FAIL strategy: " + value);
            }
    );

    private final String description;

    private final Function<Object, BigDecimal> handler;

    // Constructor is handled by @RequiredArgsConstructor

    /**
     * Handle a value according to the strategy
     *
     * @param value The value to handle
     * @return The handled value
     */
    public BigDecimal handleValue(Object value) {
        log.trace("Handling value with {} strategy: {}", this.name(), value);

        try {
            return handler.apply(value);
        } catch (Exception e) {
            log.error("Error handling value with {} strategy: {}", this.name(), value, e);
            throw e;
        }
    }

    /**
     * Get a handling strategy by name, with a default fallback
     *
     * @param name The strategy name
     * @param defaultStrategy The default strategy if not found
     * @return The handling strategy
     */
    public static HandlingStrategy fromName(String name, HandlingStrategy defaultStrategy) {
        if (name == null || name.isBlank()) {
            return defaultStrategy;
        }

        try {
            return HandlingStrategy.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown handling strategy: {}, using default: {}", name, defaultStrategy);
            return defaultStrategy;
        }
    }
}
