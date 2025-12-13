package com.indra.minsait.dvsmart.reorganization.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Author: hahuaranga@indracompany.com
 * Created on: 12-12-2025 at 11:03:22
 * File: SftpConfigProperties.java
 */

@Getter
@Setter
@ConfigurationProperties(prefix = "sftp")
public class SftpConfigProperties {
    
    private Origin origin = new Origin();
    private Destination dest = new Destination();
    
    @Getter
    @Setter
    public static class Origin {
        private String host;
        private int port = 22;
        private String user;
        private String password;
        private String baseDir;
        private Pool pool = new Pool();
        private int timeout = 30000;
    }
    
    @Getter
    @Setter
    public static class Destination {
        private String host;
        private int port = 22;
        private String user;
        private String password;
        private String baseDir;
        private Pool pool = new Pool();
        private int timeout = 30000;
    }
    
    @Getter
    @Setter
    public static class Pool {
        private int size = 10;
    }
}
