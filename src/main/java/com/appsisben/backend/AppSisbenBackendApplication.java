package com.appsisben.backend;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@ConfigurationPropertiesScan
@SpringBootApplication
public class AppSisbenBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppSisbenBackendApplication.class, args);
    }
}