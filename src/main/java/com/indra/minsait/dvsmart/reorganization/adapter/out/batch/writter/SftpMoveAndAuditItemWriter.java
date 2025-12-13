package com.indra.minsait.dvsmart.reorganization.adapter.out.batch.writter;

import com.indra.minsait.dvsmart.reorganization.application.port.out.ProcessedFileAuditRepository;
import com.indra.minsait.dvsmart.reorganization.application.port.out.SftpDestinationRepository;
import com.indra.minsait.dvsmart.reorganization.application.port.out.SftpOriginRepository;
import com.indra.minsait.dvsmart.reorganization.domain.model.ArchivoLegacy;
import com.indra.minsait.dvsmart.reorganization.domain.model.ProcessedArchivo;
import com.indra.minsait.dvsmart.reorganization.domain.service.FileReorganizationService;
import com.indra.minsait.dvsmart.reorganization.infrastructure.config.SftpConfigProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Author: hahuaranga@indracompany.com
 * Created on: 12-12-2025 at 13:22:10
 * File: SftpMoveAndAuditItemWriter.java
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class SftpMoveAndAuditItemWriter implements ItemWriter<ArchivoLegacy> {

    private final SftpOriginRepository originRepo;
    private final SftpDestinationRepository destRepo;
    private final ProcessedFileAuditRepository auditRepo;
    private final FileReorganizationService reorganizationService;
    private final SftpConfigProperties props;

    @Override
    public void write(Chunk<? extends ArchivoLegacy> chunk) {
        List<ProcessedArchivo> auditRecords = new ArrayList<>();
        
        for (ArchivoLegacy archivo : chunk) {
            ProcessedArchivo audit = processFile(archivo);
            auditRecords.add(audit);
        }
        
        if (!auditRecords.isEmpty()) {
            auditRepo.saveAll(auditRecords);
        }
    }

    private ProcessedArchivo processFile(ArchivoLegacy archivo) {
        String destinationPath = reorganizationService.calculateDestinationPath(
            archivo, props.getDest().getBaseDir());
        
        try (InputStream in = originRepo.readFile(archivo.getRutaOrigen())) {
            
            // Transferencia streaming directa (crea dirs internamente)
            destRepo.transferTo(destinationPath, in);
            
            log.debug("Successfully transferred: {} -> {}", archivo.getRutaOrigen(), destinationPath);
            
            return ProcessedArchivo.builder()
                    .idUnico(archivo.getIdUnico())
                    .rutaOrigen(archivo.getRutaOrigen())
                    .rutaDestino(destinationPath)
                    .nombre(archivo.getNombre())
                    .status("SUCCESS")
                    .processedAt(Instant.now())
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to process file: {}", archivo.getRutaOrigen(), e);
            
            return ProcessedArchivo.builder()
                    .idUnico(archivo.getIdUnico())
                    .rutaOrigen(archivo.getRutaOrigen())
                    .rutaDestino(destinationPath)
                    .nombre(archivo.getNombre())
                    .status("FAILED")
                    .processedAt(Instant.now())
                    .errorMessage(e.getMessage())
                    .build();
        }
    }
}