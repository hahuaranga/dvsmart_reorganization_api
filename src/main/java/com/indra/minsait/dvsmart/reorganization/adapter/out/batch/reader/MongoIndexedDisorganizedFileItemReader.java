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
package com.indra.minsait.dvsmart.reorganization.adapter.out.batch.reader;

import com.indra.minsait.dvsmart.reorganization.adapter.out.persistence.mongodb.entity.DisorganizedFilesIndexDocument;
import com.indra.minsait.dvsmart.reorganization.infrastructure.config.MongoConfigProperties;

import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.batch.infrastructure.item.data.MongoCursorItemReader;
import org.springframework.batch.infrastructure.item.data.builder.MongoCursorItemReaderBuilder;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

/**
 * Author: hahuaranga@indracompany.com
 * Created on: 12-12-2025 at 13:21:00
 * File: MongoIndexedFileItemReader.java
 */

/**
 * Reader que utiliza cursor streaming de MongoDB para leer millones de archivos
 * sin cargar todo en memoria ni usar skip() que es lento.
 */
@Component
@RequiredArgsConstructor
public class MongoIndexedDisorganizedFileItemReader {

    private final MongoTemplate mongoTemplate;
    
    private final MongoConfigProperties properties; 

    /**
     * Crea un MongoCursorItemReader para streaming eficiente
     * 
     * @return MongoCursorItemReader configurado
     */
    public MongoCursorItemReader<DisorganizedFilesIndexDocument> createReader() {
        // ✅ FILTRAR: Solo archivos pendientes de reorganizar
        Document query = new Document("reorg_status", "PENDING");
        
        Map<String, Sort.Direction> sorts = new HashMap<>();
        sorts.put("_id", Sort.Direction.ASC);
        
        return new MongoCursorItemReaderBuilder<DisorganizedFilesIndexDocument>()
                .name("archivoIndexCursorReader")
                .template(mongoTemplate)
                .jsonQuery(query.toJson())
                .targetType(DisorganizedFilesIndexDocument.class)
                .sorts(sorts)
                .collection(properties.getFilesIndex())
                .batchSize(100)
                .saveState(true)
                .build();
    }
}