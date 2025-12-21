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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.sftp.client.SftpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;
import org.springframework.stereotype.Repository;
import java.io.BufferedInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Author: hahuaranga@indracompany.com
 * Created on: 12-12-2025 at 13:18:21
 * File: SftpOriginRepositoryImpl.java
 */

/**
 * Implementación corregida que garantiza:
 * 1. Devolución de sesiones al pool
 * 2. Buffering para reducir llamadas de red
 * 3. Manejo robusto de errores
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SftpOriginRepositoryImpl implements SftpOriginRepository {

	@Qualifier("sftpOriginTemplate")
    private final SftpRemoteFileTemplate originTemplate;
    
    // ✅ Buffer de 8KB para reducir llamadas de red
    private static final int BUFFER_SIZE = 8192;

    @Override
    public InputStream readFile(String path) {
        Session<SftpClient.DirEntry> session = null;
        
        try {
            // ✅ CRÍTICO: Obtener sesión explícitamente del pool
            session = originTemplate.getSessionFactory().getSession();
            
            log.trace("Session acquired from pool for file: {}", path);
            
            // Abrir stream raw del archivo SFTP
            InputStream rawStream = session.readRaw(path);
            
            // ✅ Envolver en BufferedInputStream para performance
            // ✅ Envolver en SessionAwareInputStream para garantizar cierre de sesión
            return new BufferedInputStream(
                new SessionAwareInputStream(rawStream, session, path),
                BUFFER_SIZE
            );
            
        } catch (Exception e) {
            // ⚠️ Si falla antes de crear el stream, devolver sesión manualmente
            if (session != null) {
                try {
                    session.close();
                    log.debug("Session returned to pool after error");
                } catch (Exception closeEx) {
                    log.warn("Failed to close session after error", closeEx);
                }
            }
            
            log.error("Error reading file from origin SFTP: {}", path, e);
            throw new RuntimeException("Failed to read file from origin SFTP: " + path, e);
        }
    }

    /**
     * InputStream wrapper que garantiza:
     * 1. Cierre del stream SFTP raw
     * 2. Finalización del modo raw (session.finalizeRaw())
     * 3. Devolución de la sesión al pool (session.close())
     * 
     * CRÍTICO: Esta clase resuelve la fuga de sesiones del código original.
     */
    private static class SessionAwareInputStream extends FilterInputStream {
        
        private final Session<SftpClient.DirEntry> session;
        private final String path;
        private boolean closed = false;
        
        protected SessionAwareInputStream(InputStream in, 
                                           Session<SftpClient.DirEntry> session,
                                           String path) {
            super(in);
            this.session = session;
            this.path = path;
        }
        
        @Override
        public void close() throws IOException {
            if (closed) {
                return;  // Prevenir double-close
            }
            
            try {
                // 1. Cerrar el stream raw
                super.close();
                
                // 2. Finalizar modo raw en la sesión
                session.finalizeRaw();
                
                log.trace("Stream closed and finalized for file: {}", path);
                
            } catch (IOException e) {
                log.warn("Error closing stream for file: {}", path, e);
                throw e;
                
            } finally {
                // 3. ✅ CRÍTICO: Devolver sesión al pool
                try {
                    session.close();
                    log.trace("Session returned to pool after reading: {}", path);
                } catch (Exception e) {
                    log.error("Failed to return session to pool: {}", path, e);
                }
                
                closed = true;
            }
        }
        
        @Override
        protected void finalize() throws Throwable {
            // Safety net: Si el stream no se cerró explícitamente
            if (!closed) {
                log.warn("Stream was not closed properly for file: {}. " +
                         "Forcing close in finalizer.", path);
                try {
                    close();
                } catch (IOException e) {
                    log.error("Error in finalizer close", e);
                }
            }
            super.finalize();
        }
    }
}