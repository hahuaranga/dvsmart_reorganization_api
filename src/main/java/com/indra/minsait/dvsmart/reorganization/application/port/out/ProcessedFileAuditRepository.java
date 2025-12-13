package com.indra.minsait.dvsmart.reorganization.application.port.out;

import com.indra.minsait.dvsmart.reorganization.domain.model.ProcessedArchivo;
import java.util.List;

/**
 * Author: hahuaranga@indracompany.com
 * Created on: 12-12-2025 at 12:31:54
 * File: ProcessedFileAuditRepository.java
 */

public interface ProcessedFileAuditRepository {
    void save(ProcessedArchivo archivo);
    void saveAll(List<ProcessedArchivo> archivos);
}