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
package com.indra.minsait.dvsmart.reorganization.adapter.out.persistence.mongodb.entity;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

/**
 * Author: hahuaranga@indracompany.com
 * Created on: 12-12-2025 at 12:59:24
 * File: ArchivoIndexDocument.java
 */

/**
 * ✅ Documento MongoDB para colección UNIFICADA files_index.
 * Soporta tanto indexación como reorganización.
 * 
 * Los índices se crean mediante script 01_init_collections.js
 */
@Data
@Builder
@Document(collection = "files_index")  // ✅ Colección unificada
public class DisorganizedFilesIndexDocument {
    
    @Id
    private String id;
    
    // ✅ SIN @Indexed - Los índices están en el script
    private String idUnico;           // SHA-256 del path completo
    
    // ========== METADATA DEL ARCHIVO ==========
    private String sourcePath;         // Ruta en SFTP origen
    private String fileName;
    private String extension;
    private Long fileSize;
    private Instant lastModificationDate;
    
    // ========== CONTROL DE INDEXACIÓN ==========
    private String indexing_status;         // PENDING | COMPLETED | FAILED
    private Instant indexing_indexedAt;
    private String indexing_errorDescription;
    
    // ========== CONTROL DE REORGANIZACIÓN ==========
    private String reorg_status;            // PENDING | PROCESSING | SUCCESS | FAILED | SKIPPED
    private String reorg_destinationPath;
    private Instant reorg_reorganizedAt;
    private Long reorg_jobExecutionId;
    private Long reorg_durationMs;
    private Integer reorg_attempts;
    private String reorg_errorDescription;
    private Instant reorg_lastAttemptAt;
    
    // ========== METADATA DE NEGOCIO (OPCIONAL) ==========
    private String business_tipoDocumento;
    private String business_codigoCliente;
    private Integer business_anio;
    private Integer business_mes;
}