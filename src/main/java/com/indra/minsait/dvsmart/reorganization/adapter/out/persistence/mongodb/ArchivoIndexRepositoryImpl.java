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

import com.indra.minsait.dvsmart.reorganization.adapter.out.persistence.mongodb.entity.ArchivoIndexDocument;
import com.indra.minsait.dvsmart.reorganization.application.port.out.ArchivoIndexRepository;
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
public class ArchivoIndexRepositoryImpl implements ArchivoIndexRepository {

    private final MongoTemplate mongoTemplate;

    @Override
    public List<ArchivoLegacy> findAll() {
        return mongoTemplate.findAll(ArchivoIndexDocument.class).stream()
                .map(this::toModel)
                .collect(Collectors.toList());
    }

    @Override
    public long count() {
        return mongoTemplate.count(new org.springframework.data.mongodb.core.query.Query(), 
                                    ArchivoIndexDocument.class);
    }

    private ArchivoLegacy toModel(ArchivoIndexDocument doc) {
        return ArchivoLegacy.builder()
                .idUnico(doc.getIdUnico())
                .rutaOrigen(doc.getRutaOrigen())
                .nombre(doc.getNombre())
                .mtime(doc.getMtime())
                .build();
    }
}