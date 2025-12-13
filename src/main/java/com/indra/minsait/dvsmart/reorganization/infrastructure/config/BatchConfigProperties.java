package com.indra.minsait.dvsmart.reorganization.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Author: hahuaranga@indracompany.com
 * Created on: 12-12-2025 at 12:25:45
 * File: BatchConfigProperties.java
 */

@Getter
@Setter
@ConfigurationProperties(prefix = "batch")
public class BatchConfigProperties {
    private int chunkSize = 100;
    private int concurrencyLimit = 10;
    private int threadPoolSize = 20;
    private int queueCapacity = 1000;
}
