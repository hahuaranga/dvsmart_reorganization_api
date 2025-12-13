package com.indra.minsait.dvsmart.reorganization.adapter.out.persistence.mongodb.entity;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

/**
 * Author: hahuaranga@indracompany.com
 * Created on: 12-12-2025 at 13:00:35
 * File: ProcessedFileDocument.java
 */

@Data
@Builder
@Document(collection = "processed_files")
public class ProcessedFileDocument {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String idUnico;
    
    private String rutaOrigen;
    private String rutaDestino;
    private String nombre;
    private String status;
    private Instant processedAt;
    private String errorMessage;
}
