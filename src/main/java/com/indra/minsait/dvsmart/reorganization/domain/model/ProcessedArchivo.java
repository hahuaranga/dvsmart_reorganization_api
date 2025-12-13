package com.indra.minsait.dvsmart.reorganization.domain.model;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

/**
 * Author: hahuaranga@indracompany.com
 * Created on: 12-12-2025 at 12:27:53
 * File: ProcessedArchivo.java
 */

@Data
@Builder
public class ProcessedArchivo {
    private String idUnico;
    private String rutaOrigen;
    private String rutaDestino;
    private String nombre;
    private String status;
    private Instant processedAt;
    private String errorMessage;
}
