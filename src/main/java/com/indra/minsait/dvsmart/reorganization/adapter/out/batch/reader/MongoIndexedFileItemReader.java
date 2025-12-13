package com.indra.minsait.dvsmart.reorganization.adapter.out.batch.reader;

import com.indra.minsait.dvsmart.reorganization.adapter.out.persistence.mongodb.entity.ArchivoIndexDocument;
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
public class MongoIndexedFileItemReader {

    private final MongoTemplate mongoTemplate;

    /**
     * Crea un MongoCursorItemReader para streaming eficiente
     * 
     * @return MongoCursorItemReader configurado
     */
    public MongoCursorItemReader<ArchivoIndexDocument> createReader() {
        // Query vacía = todos los documentos
        Document query = new Document();
        
        // Sort por _id para orden consistente y uso de índice
        Map<String, Sort.Direction> sorts = new HashMap<>();
        sorts.put("_id", Sort.Direction.ASC);
        
        return new MongoCursorItemReaderBuilder<ArchivoIndexDocument>()
                .name("archivoIndexCursorReader")
                .template(mongoTemplate)
                .jsonQuery(query.toJson())
                .targetType(ArchivoIndexDocument.class)
                .sorts(sorts)
                .collection("archivo_index")
                .batchSize(100) // Tamaño del batch interno del cursor
                .saveState(true) // Permite restart del job
                .build();
    }
}