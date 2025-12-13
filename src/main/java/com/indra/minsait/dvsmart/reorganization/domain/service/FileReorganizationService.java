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
package com.indra.minsait.dvsmart.reorganization.domain.service;

import com.indra.minsait.dvsmart.reorganization.domain.model.ArchivoLegacy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Author: hahuaranga@indracompany.com
 * Created on: 12-12-2025 at 12:28:44
 * File: FileReorganizationService.java
 */

@Slf4j
@Service
public class FileReorganizationService {

    private static final int PARTITION_DEPTH = 3;
    private static final int CHARS_PER_LEVEL = 2;

    public String calculateDestinationPath(ArchivoLegacy archivo, String baseDir) {
        String hash = generateSHA256Hash(archivo.getRutaOrigen() + archivo.getNombre());
        String partitionPath = buildPartitionPath(hash);
        return String.format("%s/%s/%s", baseDir, partitionPath, archivo.getNombre());
    }

    private String generateSHA256Hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available", e);
            throw new RuntimeException("Failed to generate hash", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private String buildPartitionPath(String hash) {
        StringBuilder path = new StringBuilder();
        for (int i = 0; i < PARTITION_DEPTH; i++) {
            int start = i * CHARS_PER_LEVEL;
            int end = Math.min(start + CHARS_PER_LEVEL, hash.length());
            if (i > 0) {
                path.append('/');
            }
            path.append(hash.substring(start, end));
        }
        return path.toString();
    }
}
