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

import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Author: hahuaranga@indracompany.com
 * Created on: 26-12-2025 at 11:59:53
 * File: JobExecutionAudit.java
 */

/**
 * Modelo de dominio para auditoría de ejecuciones de jobs.
 * Soporta jobs con múltiples steps:
 * - Step 1: reorganization-step (copiar archivos)
 * - Step 2: cleanup-origin-step (borrar origen)
 */
@Data
@Builder
public class JobExecutionAudit {
    
    // ═══════════════════════════════════════════════════════════════
    // IDENTIFICACIÓN
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * ID único de auditoría (formato: jobName-jobExecutionId-uuid)
     * Ejemplo: "BATCH-REORG-FULL-12345-a1b2c3d4"
     */
    private String auditId;
    
    /**
     * ID de ejecución de Spring Batch
     */
    private Long jobExecutionId;
    
    // ═══════════════════════════════════════════════════════════════
    // INFORMACIÓN DEL SERVICIO Y JOB
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Nombre del microservicio
     * Ejemplo: "dvsmart-reorganization-api"
     */
    private String serviceName;
    
    /**
     * Nombre del job
     * Ejemplo: "BATCH-REORG-FULL"
     */
    private String jobName;
    
    // ═══════════════════════════════════════════════════════════════
    // TIEMPOS DE EJECUCIÓN
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Fecha/hora de inicio del job
     */
    private Instant startTime;
    
    /**
     * Fecha/hora de fin del job
     */
    private Instant endTime;
    
    /**
     * Duración total del job en milisegundos
     */
    private Long durationMs;
    
    /**
     * Duración formateada en formato legible
     * Ejemplo: "1h 45m 30s"
     */
    private String durationFormatted;
    
    // ═══════════════════════════════════════════════════════════════
    // ESTADO Y RESULTADOS
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Estado del job
     * Valores: STARTED, COMPLETED, FAILED, STOPPED
     */
    private String status;
    
    /**
     * Código de salida
     * Valores: COMPLETED, FAILED, UNKNOWN
     */
    private String exitCode;
    
    /**
     * Descripción del resultado
     */
    private String exitDescription;
    
    // ═══════════════════════════════════════════════════════════════
    // MÉTRICAS DE REORGANIZACIÓN (STEP 1)
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Total de archivos reorganizados exitosamente (Step 1: write count)
     */
    private Long totalFilesReorganized;
    
    /**
     * Total de archivos procesados (Step 1: read count)
     * Incluye reorganizados + saltados + fallidos
     */
    private Long totalFilesProcessed;
    
    /**
     * Total de archivos saltados (Step 1: skip count)
     */
    private Long totalFilesSkipped;
    
    /**
     * Total de archivos que fallaron (Step 1: write skip + rollback)
     */
    private Long totalFilesFailed;
    
    // ═══════════════════════════════════════════════════════════════
    // MÉTRICAS DE CLEANUP (STEP 2) - NUEVOS
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Total de archivos borrados del SFTP origen (Step 2: write count)
     */
    private Long totalFilesDeleted;
    
    /**
     * Total de archivos que fallaron al borrar (Step 2: write skip)
     */
    private Long totalFilesDeletionFailed;
    
    // ═══════════════════════════════════════════════════════════════
    // DESGLOSE POR STEPS - NUEVO
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Lista con el resumen de cada step ejecutado
     * Permite analizar performance individual de:
     * - reorganization-step
     * - cleanup-origin-step
     */
    private List<StepExecutionSummary> stepExecutions;
    
    // ═══════════════════════════════════════════════════════════════
    // MÉTRICAS TÉCNICAS DE SPRING BATCH (GLOBALES)
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Total de lecturas (sum de todos los steps)
     */
    private Long readCount;
    
    /**
     * Total de escrituras (sum de todos los steps)
     */
    private Long writeCount;
    
    /**
     * Total de commits (sum de todos los steps)
     */
    private Long commitCount;
    
    /**
     * Total de rollbacks (sum de todos los steps)
     */
    private Long rollbackCount;
    
    /**
     * Throughput calculado (archivos reorganizados / segundos totales)
     */
    private Double filesPerSecond;
    
    // ═══════════════════════════════════════════════════════════════
    // INFORMACIÓN DE ERRORES
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Descripción del error principal
     */
    private String errorDescription;
    
    /**
     * Stack trace truncado (primeras 10 líneas)
     */
    private String errorStackTrace;
    
    /**
     * Número de excepciones durante la ejecución
     */
    private Integer failureCount;
    
    // ═══════════════════════════════════════════════════════════════
    // PARÁMETROS DEL JOB
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Parámetros de entrada del job
     * Ejemplo: { "timestamp": "2025-12-27T10:00:00" }
     */
    private Map<String, Object> jobParameters;
    
    // ═══════════════════════════════════════════════════════════════
    // INFORMACIÓN DEL SERVIDOR
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Hostname del servidor donde se ejecutó
     */
    private String hostname;
    
    /**
     * ID de la instancia (K8s pod name o hostname)
     */
    private String instanceId;
    
    // ═══════════════════════════════════════════════════════════════
    // AUDITORÍA
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Fecha de creación del registro (beforeJob)
     */
    private Instant createdAt;
    
    /**
     * Fecha de última actualización (afterJob)
     */
    private Instant updatedAt;
}
