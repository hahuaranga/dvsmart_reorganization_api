/*
 * /////////////////////////////////////////////////////////////////////////////
 *
 * Copyright (c) 2026 Indra Sistemas, S.A. All Rights Reserved.
 * http://www.indracompany.com/
 *
 * The contents of this file are owned by Indra Sistemas, S.A. copyright holder.
 * This file can only be copied, distributed and used all or in part with the
 * written permission of Indra Sistemas, S.A, or in accordance with the terms and
 * conditions laid down in the agreement / contract under which supplied.
 *
 * /////////////////////////////////////////////////////////////////////////////
 */
package com.indra.minsait.dvsmart.reorganization.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

/**
 * Author: hahuaranga@indracompany.com
 * Created on: 28-12-2025 at 19:06:01
 * File: CleanupCandidate.java
 */

/**
 * Modelo de dominio que representa un archivo candidato para cleanup (borrado de origen).
 * 
 * Un archivo es candidato si:
 * - reorg_status = COMPLETED
 * - deleted_from_source = false
 * 
 * @author hahuaranga@indracompany.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CleanupCandidate {
    
    /**
     * ID único del archivo (hash SHA-256)
     */
    private String idUnico;
    
    /**
     * Ruta completa en SFTP origen (será borrado)
     */
    private String sourcePath;
    
    /**
     * Ruta completa en SFTP destino (debe existir)
     */
    private String destinationPath;
    
    /**
     * Fecha en que se completó la reorganización
     * Usado para validar que el archivo no fue modificado después
     */
    private Instant reorgCompletedAt;
    
    /**
     * Tamaño del archivo en bytes
     */
    private Long fileSize;
    
    /**
     * Fecha de última modificación en origen
     */
    private Instant lastModificationDate;
}
