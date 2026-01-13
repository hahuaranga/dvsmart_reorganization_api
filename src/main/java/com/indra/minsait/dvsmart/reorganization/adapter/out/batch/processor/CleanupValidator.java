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
package com.indra.minsait.dvsmart.reorganization.adapter.out.batch.processor;

import com.indra.minsait.dvsmart.reorganization.domain.model.CleanupCandidate;
import com.indra.minsait.dvsmart.reorganization.domain.model.CleanupResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * Author: hahuaranga@indracompany.com
 * Created on: 28-12-2025 at 19:12:34
 * File: CleanupValidator.java
 */

/**
 * Processor que valida candidatos para cleanup.
 * 
 * IMPORTANTE: NO usa SFTP aquí (solo validaciones en memoria).
 * Las validaciones SFTP se hacen en el Writer si es necesario.
 * 
 * Validaciones:
 * 1. Tiene fecha de reorganización
 * 2. Tiene ruta de destino
 * 3. Metadata básica completa
 * 
 * */

@Slf4j
@Component
@RequiredArgsConstructor
public class CleanupValidator implements ItemProcessor<CleanupCandidate, CleanupResult> {

    @Override
    public CleanupResult process(CleanupCandidate candidate) {
        
        // Validación 1: Verificar fecha de reorganización
        if (candidate.getReorgCompletedAt() == null) {
            log.warn("Missing reorg completion date, skipping: {}", candidate.getIdUnico());
            return null;  // Skip
        }
        
        // Validación 2: Verificar ruta de destino
        if (candidate.getDestinationPath() == null || candidate.getDestinationPath().isEmpty()) {
            log.warn("Missing destination path, skipping: {}", candidate.getIdUnico());
            return null;  // Skip
        }
        
        // Validación 3: Verificar ruta de origen
        if (candidate.getSourcePath() == null || candidate.getSourcePath().isEmpty()) {
            log.warn("Missing source path, skipping: {}", candidate.getIdUnico());
            return null;  // Skip
        }
        
        // ✅ Validaciones OK - pasar al Writer
        return CleanupResult.success(candidate);
    }
}