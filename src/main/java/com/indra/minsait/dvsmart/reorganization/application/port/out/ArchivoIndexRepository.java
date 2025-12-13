package com.indra.minsait.dvsmart.reorganization.application.port.out;

import com.indra.minsait.dvsmart.reorganization.domain.model.ArchivoLegacy;
import java.util.List;

/**
 * Author: hahuaranga@indracompany.com
 * Created on: 12-12-2025 at 12:30:42
 * File: ArchivoIndexRepository.java
 */

public interface ArchivoIndexRepository {
    List<ArchivoLegacy> findAll();
    long count();
}
