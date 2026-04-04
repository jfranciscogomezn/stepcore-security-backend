package com.gmm.devengos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class GmmDevengosApplication {

    public static void main(final String[] args) {
        SpringApplication.run(GmmDevengosApplication.class, args);
    }
}
