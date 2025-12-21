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

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

/**
 * Author: hahuaranga@indracompany.com
 * Created on: 12-12-2025 at 12:59:24
 * File: ArchivoIndexDocument.java
 */

@Data
@Document(collection = "files_index")
public class DisorganizedFilesIndexDocument {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String idUnico;
    
    // ========== METADATA DEL ARCHIVO ==========
    private String sourcePath;           // ✅ NUEVO (antes rutaOrigen)
    private String fileName;             // ✅ NUEVO (antes nombre)
    private String extension;
    private Long fileSize;               // ✅ NUEVO (antes tamanio)
    private Instant lastModificationDate; // ✅ NUEVO (antes mtime)
    
    // ========== CONTROL DE INDEXACIÓN ==========
    private String indexing_status;       // ✅ NUEVO
    private Instant indexing_indexedAt;   // ✅ NUEVO (antes indexadoEn)
    private String indexing_errorDescription; // ✅ NUEVO
    
    // ========== CONTROL DE REORGANIZACIÓN ==========
    private String reorg_status;          // ✅ NUEVO
    private String reorg_destinationPath; // ✅ NUEVO
    private Instant reorg_reorganizedAt;  // ✅ NUEVO
    private Long reorg_jobExecutionId;    // ✅ NUEVO
    private Long reorg_durationMs;        // ✅ NUEVO
    private Integer reorg_attempts;       // ✅ NUEVO
    private String reorg_errorDescription; // ✅ NUEVO
    private Instant reorg_lastAttemptAt;  // ✅ NUEVO
    
    // ========== METADATA DE NEGOCIO (OPCIONAL) ==========
    private String business_tipoDocumento; // ✅ NUEVO
    private String business_codigoCliente; // ✅ NUEVO
    private Integer business_anio;         // ✅ NUEVO
    private Integer business_mes;          // ✅ NUEVO    
}