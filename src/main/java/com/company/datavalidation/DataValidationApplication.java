package com.company.datavalidation;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
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
public class DataValidationApplication {

	public static void main(String[] args) {
		SpringApplication.run(DataValidationApplication.class, args);
	}
}
