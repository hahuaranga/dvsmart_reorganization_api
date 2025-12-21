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
package com.indra.minsait.dvsmart.reorganization.adapter.out.sftp;

import com.indra.minsait.dvsmart.reorganization.application.port.out.SftpDestinationRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.sftp.client.SftpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;
import org.springframework.stereotype.Repository;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Author: hahuaranga@indracompany.com
 * Created on: 12-12-2025 at 13:19:47
 * File: SftpDestinationRepositoryImpl.java
 */

/**
 * Implementación optimizada con:
 * 1. Buffering para reducir llamadas de red
 * 2. Creación automática de directorios
 * 3. Manejo robusto de errores
 */
@Slf4j
@Repository
public class SftpDestinationRepositoryImpl implements SftpDestinationRepository {

    private final SftpRemoteFileTemplate destinationTemplate;
    
    // Constructor manual con @Qualifier (correcto para Lombok)
    public SftpDestinationRepositoryImpl(@Qualifier("sftpDestinationTemplate") SftpRemoteFileTemplate destinationTemplate) {
        this.destinationTemplate = destinationTemplate;
    }
    
    // ✅ Buffer de 8KB para escritura eficiente
    private static final int BUFFER_SIZE = 8192;

    @Override
    public void transferTo(String remotePath, InputStream inputStream) {
        try {
            destinationTemplate.execute(session -> {
                // 1. Crear directorios padre si no existen
                createParentDirectories(session, remotePath);
                
                // 2. ✅ Envolver en BufferedInputStream para reducir I/O
                try (BufferedInputStream bufferedInput = 
                        new BufferedInputStream(inputStream, BUFFER_SIZE)) {
                    
                    // 3. Escribir archivo en destino
                    session.write(bufferedInput, remotePath);
                    
                    log.trace("File transferred successfully: {}", remotePath);
                }
                
                return null;
            });
            
        } catch (Exception e) {
            log.error("Error transferring file to destination SFTP: {}", remotePath, e);
            throw new RuntimeException("Failed to transfer file to destination SFTP: " + remotePath, e);
        }
    }

    @Override
    public void createDirectories(String path) {
        String parentPath = getParentPath(path);
        
        if (parentPath.isEmpty()) {
            return;  // No hay directorio padre
        }
        
        try {
            destinationTemplate.execute(session -> {
                createParentDirectories(session, parentPath);
                return null;
            });
            
        } catch (Exception e) {
            log.warn("Error creating directories for path: {}", path, e);
            throw new RuntimeException("Failed to create directories on destination SFTP: " + parentPath, e);
        }
    }

    /**
     * Crea recursivamente todos los directorios padre necesarios
     */
    private void createParentDirectories(Session<SftpClient.DirEntry> session, 
                                          String remotePath) throws IOException {
        String parentPath = getParentPath(remotePath);
        
        if (parentPath.isEmpty()) {
            return;  // Llegamos a la raíz
        }
        
        // Dividir path en segmentos
        String[] dirs = parentPath.split("/");
        StringBuilder currentPath = new StringBuilder();
        
        for (String dir : dirs) {
            if (dir.isEmpty()) {
                continue;  // Skip empty segments (por "/" inicial)
            }
            
            currentPath.append("/").append(dir);
            String pathToCreate = currentPath.toString();
            
            // Verificar si existe antes de crear
            if (!session.exists(pathToCreate)) {
                session.mkdir(pathToCreate);
                log.debug("Created directory: {}", pathToCreate);
            }
        }
    }

    /**
     * Extrae el path del directorio padre de una ruta completa
     * 
     * Ejemplos:
     * - "/organized/a1/b2/file.txt" → "/organized/a1/b2"
     * - "/file.txt" → ""
     * - "/dir/" → "/dir"
     */
    private String getParentPath(String fullPath) {
        int lastSlash = fullPath.lastIndexOf('/');
        
        if (lastSlash <= 0) {
            return "";  // No hay directorio padre
        }
        
        return fullPath.substring(0, lastSlash);
    }
}