package com.indra.minsait.dvsmart.reorganization.adapter.out.sftp;

import com.indra.minsait.dvsmart.reorganization.application.port.out.SftpDestinationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;
import org.springframework.stereotype.Repository;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.integration.file.remote.session.Session;
import org.apache.sshd.sftp.client.SftpClient;

/**
 * Author: hahuaranga@indracompany.com
 * Created on: 12-12-2025 at 13:19:47
 * File: SftpDestinationRepositoryImpl.java
 */

/**
 * Implementación compatible con Spring Integration v7+ (Apache MINA SSHD).
 * Usa session.write(InputStream, path) para streaming directo.
 * Crea directorios y escribe en la misma sesión (atómico y eficiente).
 */
@Slf4j
@Repository
public class SftpDestinationRepositoryImpl implements SftpDestinationRepository {

    private final SftpRemoteFileTemplate destinationTemplate;

    // Constructor manual con @Qualifier (correcto para Lombok)
    public SftpDestinationRepositoryImpl(@Qualifier("sftpDestinationTemplate") SftpRemoteFileTemplate destinationTemplate) {
        this.destinationTemplate = destinationTemplate;
    }

    @Override
    public void transferTo(String remotePath, InputStream inputStream) {
        try {
            destinationTemplate.execute(session -> {
                createParentDirectories(session, remotePath);
                session.write(inputStream, remotePath);
                return null;
            });
        } catch (Exception e) {
            log.error("Error transferring file to destination SFTP: {}", remotePath, e);
            throw new RuntimeException("Failed to transfer file to destination SFTP: " + remotePath, e);
        }
    }

    @Override
    public void createDirectories(String path) {
        String parentPath = path.substring(0, path.lastIndexOf('/'));
        if (!parentPath.isEmpty()) {
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
    }

    private void createParentDirectories(Session<SftpClient.DirEntry> session, String remotePath) throws IOException {
        String parentPath = remotePath.substring(0, remotePath.lastIndexOf('/'));
        if (parentPath.isEmpty()) {
            return;
        }
        
        String[] dirs = parentPath.split("/");
        StringBuilder currentPath = new StringBuilder();
        
        for (String dir : dirs) {
            if (!dir.isEmpty()) {
                currentPath.append("/").append(dir);
                String cp = currentPath.toString();
                if (!session.exists(cp)) {
                    session.mkdir(cp);
                    log.debug("Created directory: {}", cp);
                }
            }
        }
    }
}