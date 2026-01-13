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
import com.indra.minsait.dvsmart.reorganization.domain.model.CleanupResult;
import com.indra.minsait.dvsmart.reorganization.infrastructure.config.SftpConfigProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Author: hahuaranga@indracompany.com
 * Created on: 28-12-2025 at 19:15:01
 * File: OriginFileDeleteWriterr.java
 */

/**
 * Writer que borra archivos del SFTP origen usando SSHJ con pipelining.
 * 
 * Estrategia:
 * 1. Conectar una sola vez a SFTP origen por chunk
 * 2. Ejecutar borrados en paralelo (10 threads)
 * 3. Actualizar MongoDB en bulk
 * 
 * Performance esperado:
 * - 500 archivos/chunk
 * - ~200ms por archivo (paralelo)
 * - ~20-40 segundos por chunk
 * 
 * */
@Slf4j
@Component
@RequiredArgsConstructor
public class OriginFileDeleteWriter implements ItemWriter<CleanupResult> {

    private final SftpConfigProperties sftpConfigProps;
    private final MongoTemplate mongoTemplate;

    @Override
    public void write(Chunk<? extends CleanupResult> chunk) throws Exception {
        
        if (chunk.isEmpty()) {
            return;
        }
        
        log.info("🗑️ Deleting {} files from origin using SSHJ pipelined", chunk.size());
        
        SSHClient sshClient = new SSHClient();
        
        try {
            // 1. Conectar SSH (una sola vez por chunk)
            sshClient.addHostKeyVerifier(new PromiscuousVerifier());
            sshClient.connect(sftpConfigProps.getOrigin().getHost(), sftpConfigProps.getOrigin().getPort());
            sshClient.authPassword(sftpConfigProps.getOrigin().getUser(), sftpConfigProps.getOrigin().getPassword());
            
            // 2. Abrir sesión SFTP
            SFTPClient sftpClient = sshClient.newSFTPClient();
            
            // 3. Ejecutar deletes en paralelo (pipelined)
            ExecutorService executor = Executors.newFixedThreadPool(10);
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            
            for (CleanupResult result : chunk) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        // Borrar archivo
                        sftpClient.rm(result.getSourcePath());
                        result.setDeleted(true);
                        log.debug("✅ Deleted: {}", result.getSourcePath());
                        
                    } catch (Exception e) {
                        // No fallar el chunk completo, marcar como fallido
                        result.setDeleted(false);
                        result.setErrorMessage(e.getMessage());
                        log.error("❌ Failed to delete: {}", result.getSourcePath(), e);
                    }
                }, executor);
                
                futures.add(future);
            }
            
            // 4. Esperar a que todos completen
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
            
            executor.shutdown();
            executor.awaitTermination(30, TimeUnit.SECONDS);
            
            // 5. Actualizar MongoDB (bulk)
            updateMongoDBBatch(chunk);
            
            // 6. Log resumen
            long successCount = chunk.getItems().stream().filter(CleanupResult::isDeleted).count();
            long failedCount = chunk.size() - successCount;
            
            log.info("✅ Cleanup chunk completed: {} deleted, {} failed", successCount, failedCount);
            
        } finally {
            if (sshClient.isConnected()) {
                sshClient.close();
            }
        }
    }

    /**
     * Actualiza MongoDB en bulk para los archivos procesados
     */
    private void updateMongoDBBatch(Chunk<? extends CleanupResult> chunk) {
        BulkOperations bulkOps = mongoTemplate.bulkOps(
            BulkOperations.BulkMode.UNORDERED,
            DisorganizedFilesIndexDocument.class
        );
        
        Instant now = Instant.now();
        
        for (CleanupResult result : chunk) {
            Query query = new Query(Criteria.where("idUnico").is(result.getIdUnico()));
            
            if (result.isDeleted()) {
                // Borrado exitoso
                Update update = new Update()
                    .set("deleted_from_source", true)
                    .set("source_deletion_date", now)
                    .set("deleted_by", "cleanup-step-pipelined");
                bulkOps.updateOne(query, update);
            } else {
                // Borrado fallido - registrar error pero NO marcar como deleted
                Update update = new Update()
                    .set("reorg_errorDescription", "Cleanup failed: " + result.getErrorMessage())
                    .set("reorg_lastAttemptAt", now);
                bulkOps.updateOne(query, update);
            }
        }
        
        bulkOps.execute();
    }
}
