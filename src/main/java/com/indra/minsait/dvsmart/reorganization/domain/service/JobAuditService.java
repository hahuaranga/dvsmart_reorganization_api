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
package com.indra.minsait.dvsmart.reorganization.domain.service;

import com.indra.minsait.dvsmart.reorganization.adapter.out.persistence.mongodb.entity.JobExecutionAuditDocument;
import com.indra.minsait.dvsmart.reorganization.adapter.out.persistence.mongodb.repository.JobExecutionAuditRepository;
import com.indra.minsait.dvsmart.reorganization.domain.model.JobExecutionAudit;
import com.indra.minsait.dvsmart.reorganization.domain.model.StepExecutionSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Author: hahuaranga@indracompany.com
 * Created on: 26-12-2025 at 12:19:07
 * File: JobAuditService.java
 */

/**
 * Servicio de dominio para auditoría de ejecuciones de jobs.
 * ✅ Trabaja SOLO con modelos de dominio (JobExecutionAudit)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobAuditService {
    
    private final JobExecutionAuditRepository auditRepository;
    
    @Value("${spring.application.name:dvsmart-reorganization-api}")
    private String serviceName;
    
    /**
     * Crea un registro de auditoría al inicio del job.
     * ✅ RETORNA: Modelo de dominio
     */
    public String createAuditRecord(JobExecution jobExecution) {
        try {
            // ✅ 1. Crear modelo de dominio
            JobExecutionAudit audit = buildInitialAudit(jobExecution);
            
            // ✅ 2. Mapear a entidad de infraestructura
            JobExecutionAuditDocument document = toDocument(audit);
            
            // ✅ 3. Persistir
            auditRepository.save(document);
            
            log.info("✅ Audit record created: auditId={}, jobExecutionId={}", 
                     audit.getAuditId(), audit.getJobExecutionId());
            
            return audit.getAuditId();
            
        } catch (Exception e) {
            log.error("Failed to create audit record for job execution: {}", 
                      jobExecution.getId(), e);
            return null;
        }
    }
    
    /**
     * Actualiza el registro de auditoría al finalizar el job.
     * ✅ ACTUALIZADO: Procesa TODOS los steps (reorganization + cleanup)
     */
    public void updateAuditRecord(JobExecution jobExecution) {
        try {
            Long jobExecutionId = jobExecution.getId();
            
            JobExecutionAuditDocument document = auditRepository
                    .findByJobExecutionId(jobExecutionId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Audit record not found for job execution: " + jobExecutionId));
            
            // ✅ NUEVO: Guardar el _id original
            String originalId = document.getId();
            
            JobExecutionAudit audit = toDomain(document);
            
            updateAuditWithJobMetrics(audit, jobExecution);
            
            document = toDocument(audit);
            
            // ✅ NUEVO: Restaurar el _id para que save() haga UPDATE
            document.setId(originalId);
            
            auditRepository.save(document);
            
            log.info("✅ Audit record updated: auditId={}, status={}, steps={}, filesReorganized={}, filesDeleted={}, duration={}", 
                     audit.getAuditId(), 
                     audit.getStatus(),
                     audit.getStepExecutions() != null ? audit.getStepExecutions().size() : 0,
                     audit.getTotalFilesReorganized(),
                     audit.getTotalFilesDeleted(),
                     audit.getDurationFormatted());
            
        } catch (Exception e) {
            log.error("Failed to update audit record for job execution: {}", 
                      jobExecution.getId(), e);
        }
    }
    
    // ========================================
    // MÉTODOS PRIVADOS - LÓGICA DE DOMINIO
    // ========================================
    
    /**
     * ✅ Construye el modelo de dominio inicial.
     */
    private JobExecutionAudit buildInitialAudit(JobExecution jobExecution) {
        String auditId = generateAuditId(jobExecution);
        
        return JobExecutionAudit.builder()
                .auditId(auditId)
                .jobExecutionId(jobExecution.getId())
                .serviceName(serviceName)
                .jobName(jobExecution.getJobInstance().getJobName())
                .startTime(toInstant(jobExecution.getStartTime()))
                .status(jobExecution.getStatus().name())
                .jobParameters(extractJobParameters(jobExecution))
                .hostname(getHostname())
                .instanceId(getInstanceId())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
    
    /**
     * ✅ Actualiza el modelo de dominio con métricas del job.
     * ✅ ACTUALIZADO: Procesa TODOS los steps y los separa por tipo
     */
    private void updateAuditWithJobMetrics(JobExecutionAudit audit, JobExecution jobExecution) {
        
        // ═══════════════════════════════════════════════════════════════
        // 1. INFORMACIÓN BÁSICA DEL JOB
        // ═══════════════════════════════════════════════════════════════
        
        LocalDateTime endTimeLocal = jobExecution.getEndTime();
        Instant endTime = toInstant(endTimeLocal);
        
        audit.setEndTime(endTime);
        audit.setStatus(jobExecution.getStatus().name());
        audit.setExitCode(jobExecution.getExitStatus().getExitCode());
        audit.setExitDescription(jobExecution.getExitStatus().getExitDescription());
        
        // Calcular duración total del job
        if (endTime != null && audit.getStartTime() != null) {
            long durationMs = Duration.between(audit.getStartTime(), endTime).toMillis();
            audit.setDurationMs(durationMs);
            audit.setDurationFormatted(formatDuration(durationMs));
        }
        
        // ═══════════════════════════════════════════════════════════════
        // 2. PROCESAR TODOS LOS STEPS
        // ═══════════════════════════════════════════════════════════════
        
        Collection<StepExecution> stepExecutions = jobExecution.getStepExecutions();
        
        if (!stepExecutions.isEmpty()) {
            
            // Variables para acumular métricas
            long totalReorganized = 0;
            long totalProcessed = 0;
            long totalSkipped = 0;
            long totalFailed = 0;
            long totalDeleted = 0;
            long totalDeletionFailed = 0;
            long globalReadCount = 0;
            long globalWriteCount = 0;
            long globalCommitCount = 0;
            long globalRollbackCount = 0;
            
            List<StepExecutionSummary> stepSummaries = new ArrayList<>();
            
            // ✅ Iterar sobre CADA step ejecutado
            for (StepExecution step : stepExecutions) {
                
                // Crear resumen del step
                StepExecutionSummary summary = buildStepSummary(step);
                stepSummaries.add(summary);
                
                // ✅ Identificar qué step es cuál y acumular métricas específicas
                String stepName = step.getStepName();
                
                if ("reorganization-step".equals(stepName)) {
                    // ═════════════════════════════════════════════════
                    // STEP 1: REORGANIZATION
                    // ═════════════════════════════════════════════════
                    
                    totalProcessed = step.getReadCount();
                    totalReorganized = step.getWriteCount();
                    totalSkipped = step.getReadSkipCount() + 
                                 step.getProcessSkipCount() + 
                                 step.getFilterCount();
                    totalFailed = step.getWriteSkipCount() + step.getRollbackCount();
                    
                    globalReadCount += step.getReadCount();
                    globalWriteCount += step.getWriteCount();
                    globalCommitCount += step.getCommitCount();
                    globalRollbackCount += step.getRollbackCount();
                    
                } else if ("cleanup-origin-step".equals(stepName)) {
                    // ═════════════════════════════════════════════════
                    // STEP 2: CLEANUP
                    // ═════════════════════════════════════════════════
                    
                    totalDeleted = step.getWriteCount();
                    totalDeletionFailed = step.getWriteSkipCount();
                    
                    // No acumular en global para no duplicar
                    // (cleanup opera sobre los mismos archivos que reorganization)
                    
                } else {
                    // ═════════════════════════════════════════════════
                    // OTROS STEPS (por si hay más en el futuro)
                    // ═════════════════════════════════════════════════
                    
                    log.warn("Unknown step encountered: {}", stepName);
                    globalReadCount += step.getReadCount();
                    globalWriteCount += step.getWriteCount();
                }
            }
            
            // ✅ Guardar resúmenes de steps
            audit.setStepExecutions(stepSummaries);
            
            // ✅ Guardar métricas específicas de reorganización (Step 1)
            audit.setTotalFilesReorganized(totalReorganized);
            audit.setTotalFilesProcessed(totalProcessed);
            audit.setTotalFilesSkipped(totalSkipped);
            audit.setTotalFilesFailed(totalFailed);
            
            // ✅ Guardar métricas específicas de cleanup (Step 2)
            audit.setTotalFilesDeleted(totalDeleted);
            audit.setTotalFilesDeletionFailed(totalDeletionFailed);
            
            // ✅ Guardar métricas globales (contadores técnicos de Spring Batch)
            audit.setReadCount(globalReadCount);
            audit.setWriteCount(globalWriteCount);
            audit.setCommitCount(globalCommitCount);
            audit.setRollbackCount(globalRollbackCount);
            
            // Calcular throughput (basado en archivos reorganizados)
            if (audit.getDurationMs() != null && audit.getDurationMs() > 0 && totalReorganized > 0) {
                double filesPerSecond = (double) totalReorganized / (audit.getDurationMs() / 1000.0);
                audit.setFilesPerSecond(filesPerSecond);
            }
        }
        
        // ═══════════════════════════════════════════════════════════════
        // 3. CAPTURAR ERRORES
        // ═══════════════════════════════════════════════════════════════
        
        if (!jobExecution.getAllFailureExceptions().isEmpty()) {
            Throwable firstException = jobExecution.getAllFailureExceptions().get(0);
            audit.setErrorDescription(firstException.getMessage());
            audit.setErrorStackTrace(truncateStackTrace(firstException));
            audit.setFailureCount(jobExecution.getAllFailureExceptions().size());
        }
        
        audit.setUpdatedAt(Instant.now());
    }
    
    /**
     * ✅ NUEVO: Construye un resumen de un step individual
     */
    private StepExecutionSummary buildStepSummary(StepExecution step) {
        
        // Calcular duración del step
        String stepDuration = null;
        if (step.getStartTime() != null && step.getEndTime() != null) {
            long durationMs = Duration.between(
                step.getStartTime(), 
                step.getEndTime()
            ).toMillis();
            stepDuration = formatDuration(durationMs);
        } else if (step.getStartTime() != null) {
            // Step aún en ejecución o sin endTime
            stepDuration = "in progress";
        }
        
        return StepExecutionSummary.builder()
                .stepName(step.getStepName())
                .status(step.getStatus().name())
                .readCount((int)step.getReadCount())
                .writeCount((int)step.getWriteCount())
                .skipCount(
                		(int)step.getReadSkipCount() + 
                		(int)step.getProcessSkipCount() + 
                		(int)step.getWriteSkipCount()
                )
                .duration(stepDuration)
                .build();
    }
    
    /**
     * Genera un ID único para auditoría.
     */
    private String generateAuditId(JobExecution jobExecution) {
        return String.format("%s-%d-%s",
                jobExecution.getJobInstance().getJobName(),
                jobExecution.getId(),
                UUID.randomUUID().toString().substring(0, 8));
    }
    
    /**
     * Extrae parámetros del job como Map.
     */
    private Map<String, Object> extractJobParameters(JobExecution jobExecution) {
        Map<String, Object> params = new HashMap<>();
        
        jobExecution.getJobParameters().parameters().forEach(jobParameter -> {
            params.put(jobParameter.name(), jobParameter.value());
        });
        
        return params;
    }
    
    /**
     * Convierte LocalDateTime a Instant.
     */
    private Instant toInstant(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    }
    
    /**
     * Obtiene el hostname del servidor.
     */
    private String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    /**
     * Obtiene el ID de la instancia (K8s pod name o hostname).
     */
    private String getInstanceId() {
        String podName = System.getenv("HOSTNAME");
        if (podName != null && !podName.isEmpty()) {
            return podName;
        }
        return getHostname();
    }
    
    /**
     * Formatea la duración en formato legible.
     */
    private String formatDuration(long durationMs) {
        Duration duration = Duration.ofMillis(durationMs);
        
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    /**
     * Trunca el stack trace para no saturar la BD.
     */
    private String truncateStackTrace(Throwable throwable) {
        if (throwable == null) return null;
        
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.getClass().getName()).append(": ")
          .append(throwable.getMessage()).append("\n");
        
        StackTraceElement[] elements = throwable.getStackTrace();
        int limit = Math.min(elements.length, 10);
        
        for (int i = 0; i < limit; i++) {
            sb.append("\tat ").append(elements[i].toString()).append("\n");
        }
        
        if (elements.length > limit) {
            sb.append("\t... ").append(elements.length - limit).append(" more");
        }
        
        String result = sb.toString();
        
        if (result.length() > 2000) {
            return result.substring(0, 1997) + "...";
        }
        
        return result;
    }
    
    // ========================================
    // MAPPERS - DOMINIO ↔ INFRAESTRUCTURA
    // ========================================
    
    /**
     * ✅ Mapea modelo de dominio → entidad MongoDB.
     * ✅ ACTUALIZADO: Incluye campos de cleanup y stepExecutions
     */
    private JobExecutionAuditDocument toDocument(JobExecutionAudit audit) {
        return JobExecutionAuditDocument.builder()
                .auditId(audit.getAuditId())
                .jobExecutionId(audit.getJobExecutionId())
                .serviceName(audit.getServiceName())
                .jobName(audit.getJobName())
                .startTime(audit.getStartTime())
                .endTime(audit.getEndTime())
                .durationMs(audit.getDurationMs())
                .durationFormatted(audit.getDurationFormatted())
                .status(audit.getStatus())
                .exitCode(audit.getExitCode())
                .exitDescription(audit.getExitDescription())
                
                // Métricas de reorganización
                .totalFilesReorganized(audit.getTotalFilesReorganized())
                .totalFilesProcessed(audit.getTotalFilesProcessed())
                .totalFilesSkipped(audit.getTotalFilesSkipped())
                .totalFilesFailed(audit.getTotalFilesFailed())
                
                // ✅ NUEVO: Métricas de cleanup
                .totalFilesDeleted(audit.getTotalFilesDeleted())
                .totalFilesDeletionFailed(audit.getTotalFilesDeletionFailed())
                
                // Métricas técnicas de Spring Batch
                .readCount(audit.getReadCount())
                .writeCount(audit.getWriteCount())
                .commitCount(audit.getCommitCount())
                .rollbackCount(audit.getRollbackCount())
                .filesPerSecond(audit.getFilesPerSecond())
                
                // ✅ NUEVO: Desglose por steps
                .stepExecutions(audit.getStepExecutions())
                
                // Información de errores
                .errorDescription(audit.getErrorDescription())
                .errorStackTrace(audit.getErrorStackTrace())
                .failureCount(audit.getFailureCount())
                
                // Metadata
                .jobParameters(audit.getJobParameters())
                .hostname(audit.getHostname())
                .instanceId(audit.getInstanceId())
                .createdAt(audit.getCreatedAt())
                .updatedAt(audit.getUpdatedAt())
                .build();
    }
    
    /**
     * ✅ Mapea entidad MongoDB → modelo de dominio.
     * ✅ ACTUALIZADO: Incluye campos de cleanup y stepExecutions
     */
    private JobExecutionAudit toDomain(JobExecutionAuditDocument document) {
        return JobExecutionAudit.builder()
                .auditId(document.getAuditId())
                .jobExecutionId(document.getJobExecutionId())
                .serviceName(document.getServiceName())
                .jobName(document.getJobName())
                .startTime(document.getStartTime())
                .endTime(document.getEndTime())
                .durationMs(document.getDurationMs())
                .durationFormatted(document.getDurationFormatted())
                .status(document.getStatus())
                .exitCode(document.getExitCode())
                .exitDescription(document.getExitDescription())
                
                // Métricas de reorganización
                .totalFilesReorganized(document.getTotalFilesReorganized())
                .totalFilesProcessed(document.getTotalFilesProcessed())
                .totalFilesSkipped(document.getTotalFilesSkipped())
                .totalFilesFailed(document.getTotalFilesFailed())
                
                // ✅ NUEVO: Métricas de cleanup
                .totalFilesDeleted(document.getTotalFilesDeleted())
                .totalFilesDeletionFailed(document.getTotalFilesDeletionFailed())
                
                // Métricas técnicas de Spring Batch
                .readCount(document.getReadCount())
                .writeCount(document.getWriteCount())
                .commitCount(document.getCommitCount())
                .rollbackCount(document.getRollbackCount())
                .filesPerSecond(document.getFilesPerSecond())
                
                // ✅ NUEVO: Desglose por steps
                .stepExecutions(document.getStepExecutions())
                
                // Información de errores
                .errorDescription(document.getErrorDescription())
                .errorStackTrace(document.getErrorStackTrace())
                .failureCount(document.getFailureCount())
                
                // Metadata
                .jobParameters(document.getJobParameters())
                .hostname(document.getHostname())
                .instanceId(document.getInstanceId())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }
}