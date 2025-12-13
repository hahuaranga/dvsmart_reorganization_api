package com.indra.minsait.dvsmart.reorganization.application.port.out;

import java.io.InputStream;

/**
 * Author: hahuaranga@indracompany.com
 * Created on: 12-12-2025 at 12:33:58
 * File: SftpDestinationRepository.java
 */

/**
 * Puerto actualizado para transferencia streaming sin exponer OutputStream.
 */
public interface SftpDestinationRepository {
    /**
     * Transfiere el contenido del InputStream al path remoto.
     * Crea directorios necesarios automáticamente.
     */
    void transferTo(String remotePath, InputStream inputStream);

    void createDirectories(String path);  // Mantenido por si se necesita separado
}
