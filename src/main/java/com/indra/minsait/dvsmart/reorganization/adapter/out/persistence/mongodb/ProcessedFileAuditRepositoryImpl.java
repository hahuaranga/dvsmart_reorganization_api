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
package com.indra.minsait.dvsmart.reorganization.adapter.out.persistence.mongodb;

import com.indra.minsait.dvsmart.reorganization.adapter.out.persistence.mongodb.entity.ProcessedFileDocument;
import com.indra.minsait.dvsmart.reorganization.application.port.out.ProcessedFileAuditRepository;
import com.indra.minsait.dvsmart.reorganization.domain.model.ProcessedArchivo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Author: hahuaranga@indracompany.com
 * Created on: 12-12-2025 at 13:04:12
 * File: ProcessedFileAuditRepositoryImpl.java
 */

@Repository
@RequiredArgsConstructor
public class ProcessedFileAuditRepositoryImpl implements ProcessedFileAuditRepository {

    private final MongoTemplate mongoTemplate;

    @Override
    public void save(ProcessedArchivo archivo) {
        mongoTemplate.save(toDocument(archivo));
    }

    @Override
    public void saveAll(List<ProcessedArchivo> archivos) {
        List<ProcessedFileDocument> docs = archivos.stream()
                .map(this::toDocument)
                .collect(Collectors.toList());
        mongoTemplate.insert(docs, ProcessedFileDocument.class);
    }

    private ProcessedFileDocument toDocument(ProcessedArchivo archivo) {
        return ProcessedFileDocument.builder()
                .idUnico(archivo.getIdUnico())
                .rutaOrigen(archivo.getRutaOrigen())
                .rutaDestino(archivo.getRutaDestino())
                .nombre(archivo.getNombre())
                .status(archivo.getStatus())
                .processedAt(archivo.getProcessedAt())
                .errorMessage(archivo.getErrorMessage())
                .build();
    }
}
