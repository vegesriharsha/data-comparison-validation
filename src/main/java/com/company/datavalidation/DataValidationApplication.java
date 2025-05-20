package com.company.datavalidation;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@OpenAPIDefinition(
		info = @Info(
				title = "Data Validation API",
				version = "1.0",
				description = "API for data validation framework"
		)
)
@Slf4j
public class DataValidationApplication {

	private static final String BANNER = """
            _____        _        __      __   _ _     _       _   _             
           |  __ \\      | |       \\ \\    / /  | (_)   | |     | | (_)            
           | |  | | __ _| |_ __ _  \\ \\  / /_ _| |_  __| | __ _| |_ _  ___  _ __  
           | |  | |/ _` | __/ _` |  \\ \\/ / _` | | |/ _` |/ _` | __| |/ _ \\| '_ \\ 
           | |__| | (_| | || (_| |   \\  / (_| | | | (_| | (_| | |_| | (_) | | | |
           |_____/ \\__,_|\\__\\__,_|    \\/ \\__,_|_|_|\\__,_|\\__,_|\\__|_|\\___/|_| |_|
                                                                    v1.0.0 (Java 21)
            """;

	public static void main(String[] args) {
		log.info(BANNER);
		log.info("Starting Data Validation Framework...");

		ConfigurableApplicationContext context = SpringApplication.run(DataValidationApplication.class, args);

		log.info("Data Validation Framework started successfully");
	}
}
