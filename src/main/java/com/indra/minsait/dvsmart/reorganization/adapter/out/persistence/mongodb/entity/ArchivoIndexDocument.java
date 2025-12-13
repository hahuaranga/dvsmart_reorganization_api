package com.indra.minsait.dvsmart.reorganization.adapter.out.persistence.mongodb.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

/**
 * Author: hahuaranga@indracompany.com
 * Created on: 12-12-2025 at 12:59:24
 * File: ArchivoIndexDocument.java
 */

@Data
@Document(collection = "archivo_index")
public class ArchivoIndexDocument {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String idUnico;
    
    private String rutaOrigen;
    private String nombre;
    private Instant mtime;
}