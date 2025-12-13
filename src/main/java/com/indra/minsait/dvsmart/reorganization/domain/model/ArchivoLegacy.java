package com.indra.minsait.dvsmart.reorganization.domain.model;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

/**
 * Author: hahuaranga@indracompany.com
 * Created on: 12-12-2025 at 12:26:43
 * File: ArchivoLegacy.java
 */

@Data
@Builder
public class ArchivoLegacy {
    private String idUnico;
    private String rutaOrigen;
    private String nombre;
    private Instant mtime;
}