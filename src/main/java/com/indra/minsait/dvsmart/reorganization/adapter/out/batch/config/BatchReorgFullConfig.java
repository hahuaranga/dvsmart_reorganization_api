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
package com.indra.minsait.dvsmart.reorganization.adapter.out.batch.config;

import com.indra.minsait.dvsmart.reorganization.adapter.out.batch.reader.MongoIndexedDisorganizedFileItemReader;
import com.indra.minsait.dvsmart.reorganization.adapter.out.batch.writter.SftpMoveAndIndexItemWriter;
import com.indra.minsait.dvsmart.reorganization.adapter.out.persistence.mongodb.entity.DisorganizedFilesIndexDocument;
import com.indra.minsait.dvsmart.reorganization.domain.model.ArchivoLegacy;
import com.indra.minsait.dvsmart.reorganization.domain.service.FileReorganizationService;
import com.indra.minsait.dvsmart.reorganization.infrastructure.config.BatchConfigProperties;
import com.indra.minsait.dvsmart.reorganization.infrastructure.config.SftpConfigProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.data.MongoCursorItemReader;
import org.springframework.batch.infrastructure.item.support.CompositeItemProcessor;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.concurrent.Future;

/**
 * Author: hahuaranga@indracompany.com
 * Created on: 12-12-2025 at 13:23:54
 * File: BatchReorgFullConfig.java
 */

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BatchReorgFullConfig {

    private final JobRepository jobRepository;
    private final MongoIndexedDisorganizedFileItemReader mongoReader;
    private final SftpMoveAndIndexItemWriter sftpWriter;
    private final FileReorganizationService reorganizationService;
    private final SftpConfigProperties sftpProps;
    private final BatchConfigProperties batchProps;

    // ========================================================================
    // NUEVOS BEANS AGREGADOS PARA JobOperator
    // ========================================================================
    
    /**
     * JobRegistry para que JobOperator pueda encontrar jobs por nombre.
     * Sin este bean, JobOperator lanzará NoSuchJobException.
     */
    @Bean
    JobRegistry jobRegistry() {
        return new MapJobRegistry();
    }

    // ========================================================================
    // BEANS EXISTENTES (SIN CAMBIOS)
    // ========================================================================

    @Bean(name = "batchTaskExecutor")
    TaskExecutor batchTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(batchProps.getThreadPoolSize());
        executor.setMaxPoolSize(batchProps.getThreadPoolSize());
        executor.setQueueCapacity(batchProps.getQueueCapacity());
        executor.setThreadNamePrefix("batch-reorg-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * Reader con cursor streaming
     * Usa MongoCursorItemReader para millones de registros
     */
    @Bean
    MongoCursorItemReader<DisorganizedFilesIndexDocument> archivoIndexReader() {
        return mongoReader.createReader();
    }

    /**
     * Processor 1: Convierte Document MongoDB → ArchivoLegacy
     */
    @Bean
    ItemProcessor<DisorganizedFilesIndexDocument, ArchivoLegacy> documentToLegacyProcessor() {
        return doc -> {
            if (doc == null) {
                return null;
            }
            return ArchivoLegacy.builder()
                    .idUnico(doc.getIdUnico())
                    .rutaOrigen(doc.getSourcePath())          // ✅ CAMBIO
                    .nombre(doc.getFileName())                // ✅ CAMBIO
                    .mtime(doc.getLastModificationDate())     // ✅ CAMBIO
                    .build();
        };
    }

    /**
     * Processor 2: Calcula hash partition (SHA-256)
     */
    @Bean
    ItemProcessor<ArchivoLegacy, ArchivoLegacy> hashPartitionProcessor() {
        return archivo -> {
            if (archivo == null) {
                return null;
            }
            String destPath = reorganizationService.calculateDestinationPath(
                archivo, sftpProps.getDest().getBaseDir());
            log.trace("Calculated destination: {} -> {}", archivo.getRutaOrigen(), destPath);
            return archivo;
        };
    }

    /**
     * Composite Processor: Combina Document→Legacy + Hash
     */
    @Bean
    CompositeItemProcessor<DisorganizedFilesIndexDocument, ArchivoLegacy> compositeProcessor() {
        CompositeItemProcessor<DisorganizedFilesIndexDocument, ArchivoLegacy> processor = new CompositeItemProcessor<>();
        processor.setDelegates(Arrays.asList(
            documentToLegacyProcessor(),
            hashPartitionProcessor()
        ));
        return processor;
    }

    /**
     * Async Processor para procesamiento paralelo
     */
    @Bean
    AsyncItemProcessor<DisorganizedFilesIndexDocument, ArchivoLegacy> asyncProcessor() {
        AsyncItemProcessor<DisorganizedFilesIndexDocument, ArchivoLegacy> asyncProcessor = 
            new AsyncItemProcessor<DisorganizedFilesIndexDocument, ArchivoLegacy>(compositeProcessor());
        asyncProcessor.setTaskExecutor(batchTaskExecutor());
        return asyncProcessor;
    }

    /**
     * Async Writer para escritura paralela
     */
    @Bean
    AsyncItemWriter<ArchivoLegacy> asyncWriter() {
        AsyncItemWriter<ArchivoLegacy> asyncWriter = new AsyncItemWriter<>(sftpWriter);
        return asyncWriter;
    }

    /**
     * Step principal con chunk-oriented processing
     */
    @Bean
    Step reorganizeStep() {
        return new StepBuilder("reorganizeStep", jobRepository)
                .<DisorganizedFilesIndexDocument, Future<ArchivoLegacy>>chunk(batchProps.getChunkSize())
                .reader(archivoIndexReader())
                .processor(asyncProcessor())
                .writer(asyncWriter())
                
                // ✅ 1. Activar tolerancia a fallos
                .faultTolerant()
                
                // ✅ 2. Skip de excepciones específicas
                .skip(IOException.class)
                .skip(SocketTimeoutException.class)
                .skipLimit(batchProps.getSkipLimit())
                
                // ✅ 3. Retry policy (SIN backOffPolicy - no existe ese método)
                .retry(SocketTimeoutException.class)
                .retry(SocketException.class)
                .retryLimit(batchProps.getRetryLimit())
                
                // ✅ 4. Listener correcto (ExitStatus, no void)
                .listener(new StepExecutionListener() {
                    @Override
                    public ExitStatus afterStep(StepExecution stepExecution) {
                        log.warn("Step finished - Skipped: {}, Read: {}, Written: {}", 
                            stepExecution.getSkipCount(),
                            stepExecution.getReadCount(),
                            stepExecution.getWriteCount());
                        return stepExecution.getExitStatus();
                    }
                })            
                .build();
    }

    /**
     * Job completo de reorganización.
     * IMPORTANTE: El nombre "BATCH-REORG-FULL" debe coincidir con el usado en JobOperator.start()
     */
    @Bean(name = "batchReorgFullJob")
    Job batchReorgFullJob() {
        return new JobBuilder("BATCH-REORG-FULL", jobRepository)
                .start(reorganizeStep())
                .build();
    }
}