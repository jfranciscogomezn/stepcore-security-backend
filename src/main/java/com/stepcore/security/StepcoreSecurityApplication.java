package com.stepcore.security;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class StepcoreSecurityApplication {

    public static void main(final String[] args) {
        SpringApplication.run(StepcoreSecurityApplication.class, args);
    }
}
