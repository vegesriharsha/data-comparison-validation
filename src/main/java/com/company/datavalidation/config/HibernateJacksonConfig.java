package com.company.datavalidation.config;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.hibernate5.jakarta.Hibernate5JakartaModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Hibernate-specific Jackson serialization.
 * This ensures proper handling of lazy-loaded relations and other
 * Hibernate-specific types.
 */
@Configuration
public class HibernateJacksonConfig {

    /**
     * Provides a Hibernate module for Jackson to handle Hibernate-specific types
     * and lazy loading properly.
     *
     * @return the Hibernate module for Jackson
     */
    @Bean
    public Module hibernateModule() {
        Hibernate5JakartaModule module = new Hibernate5JakartaModule();
        // Configure the module to handle lazy-loaded associations
        // Force lazy-loaded properties to be serialized as null instead of trying
        // to fetch them, which could lead to LazyInitializationException
        module.configure(Hibernate5JakartaModule.Feature.FORCE_LAZY_LOADING, false);
        return module;
    }

    /**
     * Register the Hibernate module with the ObjectMapper.
     * This method is called after the ObjectMapper is created.
     *
     * @param objectMapper the ObjectMapper bean
     */
    @Bean
    public ObjectMapper configureHibernateModule(ObjectMapper objectMapper) {
        objectMapper.registerModule(hibernateModule());
        return objectMapper;
    }
}
