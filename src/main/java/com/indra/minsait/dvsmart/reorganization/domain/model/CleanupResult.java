/*
 * /////////////////////////////////////////////////////////////////////////////
 *
 * Copyright (c) 2025 Indra Sistemas, S.A. All Rights Reserved.
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

/**
 * Author: hahuaranga@indracompany.com
 * Created on: 28-12-2025 at 19:06:52
 * File: CleanupResult.java
 */

/**
 * Resultado del proceso de cleanup (borrado de archivo de origen).
 * 
 * @author hahuaranga@indracompany.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CleanupResult {
    
    /**
     * ID único del archivo
     */
    private String idUnico;
    
    /**
     * Ruta que se intentó borrar
     */
    private String sourcePath;
    
    /**
     * Indica si el borrado fue exitoso
     */
    private boolean deleted;
    
    /**
     * Mensaje de error si falló el borrado
     */
    private String errorMessage;
    
    /**
     * Factory method para resultado exitoso
     */
    public static CleanupResult success(CleanupCandidate candidate) {
        return CleanupResult.builder()
            .idUnico(candidate.getIdUnico())
            .sourcePath(candidate.getSourcePath())
            .deleted(true)
            .build();
    }
    
    /**
     * Factory method para resultado fallido
     */
    public static CleanupResult failure(CleanupCandidate candidate, String error) {
        return CleanupResult.builder()
            .idUnico(candidate.getIdUnico())
            .sourcePath(candidate.getSourcePath())
            .deleted(false)
            .errorMessage(error)
            .build();
    }
}
