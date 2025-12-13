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

import com.indra.minsait.dvsmart.reorganization.application.port.out.SftpOriginRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;
import org.springframework.stereotype.Repository;
import java.io.InputStream;

/**
 * Author: hahuaranga@indracompany.com
 * Created on: 12-12-2025 at 13:18:21
 * File: SftpOriginRepositoryImpl.java
 */

/**
 * Implementación usando SftpRemoteFileTemplate (compatible con Apache MINA SSHD en Spring Integration 7+).
 */
@Slf4j
@Repository
public class SftpOriginRepositoryImpl implements SftpOriginRepository {

    private final SftpRemoteFileTemplate originTemplate;

    // Constructor manual con @Qualifier (correcto para Lombok)
    public SftpOriginRepositoryImpl(@Qualifier("sftpOriginTemplate") SftpRemoteFileTemplate originTemplate) {
        this.originTemplate = originTemplate;
    }

    @Override
    public InputStream readFile(String path) {
        try {
            // readRaw retorna InputStream; finalizeRaw se llama automáticamente al cerrar el stream
            return originTemplate.execute(session -> {
                InputStream rawStream = session.readRaw(path);
                // Wrapping para asegurar finalizeRaw al cerrar el stream externo
                return new InputStream() {
                    @Override
                    public int read() throws java.io.IOException {
                        return rawStream.read();
                    }

                    @Override
                    public void close() throws java.io.IOException {
                        try {
                            rawStream.close();
                        } finally {
                            session.finalizeRaw();
                        }
                    }
                };
            });
        } catch (Exception e) {
            log.error("Error reading file from origin SFTP: {}", path, e);
            throw new RuntimeException("Failed to read file from origin SFTP: " + path, e);
        }
    }
}