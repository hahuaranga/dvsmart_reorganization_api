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
package com.indra.minsait.dvsmart.reorganization.adapter.out.persistence.mongodb;

import com.indra.minsait.dvsmart.reorganization.adapter.out.persistence.mongodb.entity.DisorganizedFilesIndexDocument;
import com.indra.minsait.dvsmart.reorganization.application.port.out.DisorganizedFilesIndexRepository;
import com.indra.minsait.dvsmart.reorganization.domain.model.ArchivoLegacy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Author: hahuaranga@indracompany.com
 * Created on: 12-12-2025 at 13:02:04
 * File: ArchivoIndexRepositoryImpl.java
 */

@Repository
@RequiredArgsConstructor
public class DisorganizedFilesIndexRepositoryImpl implements DisorganizedFilesIndexRepository {

    private final MongoTemplate mongoTemplate;

    @Override
    public List<ArchivoLegacy> findAll() {
        return mongoTemplate.findAll(DisorganizedFilesIndexDocument.class).stream()
                .map(this::toModel)
                .collect(Collectors.toList());
    }

    @Override
    public long count() {
        return mongoTemplate.count(new org.springframework.data.mongodb.core.query.Query(), 
                                    DisorganizedFilesIndexDocument.class);
    }

    private ArchivoLegacy toModel(DisorganizedFilesIndexDocument doc) {
        return ArchivoLegacy.builder()
                .idUnico(doc.getIdUnico())
                .rutaOrigen(doc.getSourcePath())             // ✅ CAMBIO
                .nombre(doc.getFileName())                   // ✅ CAMBIO
                .mtime(doc.getLastModificationDate())        // ✅ CAMBIO
                .build();
    }
}