package com.indra.minsait.dvsmart.reorganization;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Author: hahuaranga@indracompany.com
 * Created on: 11-12-2025 at 17:07:25
 * File: ServiceApplication.java
 */

@SpringBootApplication
@ConfigurationPropertiesScan
public class ServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceApplication.class, args);
    }
}
