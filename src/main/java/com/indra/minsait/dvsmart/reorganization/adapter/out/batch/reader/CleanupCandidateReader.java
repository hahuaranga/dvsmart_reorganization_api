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
package com.indra.minsait.dvsmart.reorganization.adapter.out.batch.reader;

import com.indra.minsait.dvsmart.reorganization.adapter.out.persistence.mongodb.entity.DisorganizedFilesIndexDocument;
import com.indra.minsait.dvsmart.reorganization.domain.model.CleanupCandidate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.List;

/**
 * Author: hahuaranga@indracompany.com
 * Created on: 28-12-2025 at 19:08:29
 * File: CleanupCandidateReader.java
 */

/**
 * Reader que obtiene archivos candidatos para cleanup (borrado de origen).
 * 
 * Query MongoDB:
 * - reorg_status = "COMPLETED"
 * - deleted_from_source = false
 * - reorg_completedAt existe
 * - reorg_completedAt < 90 días (seguridad)
 * 
 * Usa índice: idx_cleanup_candidates
 * */

@Slf4j
@Component
@RequiredArgsConstructor
public class CleanupCandidateReader implements ItemReader<CleanupCandidate> {

    private final MongoTemplate mongoTemplate;
    
    private Iterator<CleanupCandidate> candidateIterator;
    private boolean initialized = false;

    @Override
    public CleanupCandidate read() {
        if (!initialized) {
            initializeCandidates();
            initialized = true;
        }

        return candidateIterator.hasNext() ? candidateIterator.next() : null;
    }

    /**
     * Inicializa la lista de candidatos consultando MongoDB
     */
    private void initializeCandidates() {
        log.info("════════════════════════════════════════════════════════");
        log.info("🗑️ CLEANUP: Initializing cleanup candidates");
        
        // Fecha límite: solo archivos reorganizados en los últimos 90 días
        Instant cutoffDate = Instant.now().minus(90, ChronoUnit.DAYS);
        
        // Query optimizado con índice idx_cleanup_candidates
        Query query = new Query(
            Criteria.where("reorg_status").is("COMPLETED")
                    .and("deleted_from_source").is(false)
                    //.and("reorg_completedAt").exists(true)
                    .and("reorg_completedAt").gte(cutoffDate)
                    .and("reorg_destinationPath").exists(true).ne(null)
        );
        
        // Proyección: solo campos necesarios
        query.fields()
             .include("idUnico")
             .include("sourcePath")
             .include("reorg_destinationPath")
             .include("reorg_completedAt")
             .include("fileSize")
             .include("lastModificationDate");
        
        List<DisorganizedFilesIndexDocument> documents = 
            mongoTemplate.find(query, DisorganizedFilesIndexDocument.class);
        
        log.info("Found {} files to cleanup from origin", documents.size());
        log.info("Cutoff date: {} (files older than 90 days excluded)", cutoffDate);
        log.info("════════════════════════════════════════════════════════");
        
        candidateIterator = documents.stream()
            .map(this::toCleanupCandidate)
            .iterator();
    }

    /**
     * Convierte documento MongoDB a modelo de dominio
     */
    private CleanupCandidate toCleanupCandidate(DisorganizedFilesIndexDocument doc) {
        return CleanupCandidate.builder()
            .idUnico(doc.getIdUnico())
            .sourcePath(doc.getSourcePath())
            .destinationPath(doc.getReorg_destinationPath())
            .reorgCompletedAt(doc.getReorg_completedAt())
            .fileSize(doc.getFileSize())
            .lastModificationDate(doc.getLastModificationDate())
            .build();
    }
}
