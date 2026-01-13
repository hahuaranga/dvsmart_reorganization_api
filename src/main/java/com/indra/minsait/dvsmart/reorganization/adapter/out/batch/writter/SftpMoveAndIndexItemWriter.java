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
package com.indra.minsait.dvsmart.reorganization.adapter.out.batch.writter;

import com.indra.minsait.dvsmart.reorganization.adapter.out.persistence.mongodb.entity.DisorganizedFilesIndexDocument;
import com.indra.minsait.dvsmart.reorganization.application.port.out.SftpDestinationRepository;
import com.indra.minsait.dvsmart.reorganization.application.port.out.SftpOriginRepository;
import com.indra.minsait.dvsmart.reorganization.domain.model.ArchivoLegacy;
import com.indra.minsait.dvsmart.reorganization.domain.service.FileReorganizationService;
import com.indra.minsait.dvsmart.reorganization.infrastructure.config.SftpConfigProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;

/**
 * Author: hahuaranga@indracompany.com
 * Created on: 12-12-2025 at 13:22:10
 * File: SftpMoveAndAuditItemWriter.java
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class SftpMoveAndIndexItemWriter implements ItemWriter<ArchivoLegacy> {

    private final SftpOriginRepository originRepo;
    private final SftpDestinationRepository destRepo;
    private final MongoTemplate mongoTemplate;  // ✅ CAMBIO: Inyectar MongoTemplate
    private final FileReorganizationService reorganizationService;
    private final SftpConfigProperties props;

    @Override
    public void write(Chunk<? extends ArchivoLegacy> chunk) {
        for (ArchivoLegacy archivo : chunk) {
            long startTime = System.currentTimeMillis();
            
            try {
                String destinationPath = copyFileToDestination(archivo);
                
                long duration = System.currentTimeMillis() - startTime;
                
                // ✅ ACTUALIZAR documento con estado SUCCESS
                updateReorgStatus(
                    archivo.getIdUnico(),
                    "SUCCESS",
                    destinationPath,
                    duration,
                    null
                );
                
                log.debug("✅ Processed successfully: {} -> {} ({}ms)", 
                    archivo.getRutaOrigen(), destinationPath, duration);
                
            } catch (Exception e) {
                log.error("❌ Failed to process: {}", archivo.getIdUnico(), e);
                
                long duration = System.currentTimeMillis() - startTime;
                
                // ✅ ACTUALIZAR documento con estado FAILED
                updateReorgStatus(
                    archivo.getIdUnico(),
                    "FAILED",
                    null,
                    duration,
                    e.getMessage()
                );
            }
        }
    }

	private String copyFileToDestination(ArchivoLegacy archivo) throws IOException {
		// Calcular ruta destino
		String destinationPath = reorganizationService.calculateDestinationPath(
		    archivo, props.getDest().getBaseDir());
		
		// Transferir archivo
		try (InputStream in = originRepo.readFile(archivo.getRutaOrigen())) {
		    destRepo.transferTo(destinationPath, in);
		}
		return destinationPath;
	}

    /**
     * Actualiza el estado de reorganización en MongoDB
     */
    private void updateReorgStatus(String idUnico, String status, 
                                     String destinationPath, long durationMs, 
                                     String errorDescription) {
        Query query = Query.query(Criteria.where("idUnico").is(idUnico));
        
        Update update = new Update()
                .set("reorg_status", status)
                .inc("reorg_attempts", 1);  // Incrementar intentos
        
        if ("SUCCESS".equals(status)) {
            update.set("reorg_destinationPath", destinationPath);
            update.set("reorg_completedAt", Instant.now());
            update.set("reorg_durationMs", durationMs);
            update.set("deleted_from_source", false);
        }
        
        if (errorDescription != null) {
            update.set("reorg_errorDescription", errorDescription);
        }
        
        // Actualizar documento
        mongoTemplate.updateFirst(query, update, DisorganizedFilesIndexDocument.class);
    }
}