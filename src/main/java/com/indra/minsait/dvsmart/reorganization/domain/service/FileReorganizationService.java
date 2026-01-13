/*
 * /////////////////////////////////////////////////////////////////////////////
 *
 * Copyright (c) 2026 Indra Sistemas, S.A. All Rights Reserved.
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
import com.indra.minsait.dvsmart.reorganization.infrastructure.config.SftpConfigProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Author: hahuaranga@indracompany.com
 * Created on: 12-12-2025 at 12:28:44
 * File: FileReorganizationService.java
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class FileReorganizationService {

	private final SftpConfigProperties props;

    public String calculateDestinationPath(ArchivoLegacy archivo, String baseDir) {
    	String partitionPath = buildPartitionPath(archivo.getIdUnico());
        return String.format("%s/%s/%s", baseDir, partitionPath, archivo.getNombre());
    }

    private String buildPartitionPath(String hash) {
        StringBuilder path = new StringBuilder();
        for (int i = 0; i < props.getHashPartitionig().getPartitionDepth(); i++) {
            int start = i * props.getHashPartitionig().getCharsPerLevel();
            int end = Math.min(start + props.getHashPartitionig().getCharsPerLevel(), hash.length());
            if (i > 0) {
                path.append('/');
            }
            path.append(hash.substring(start, end));
        }
        return path.toString();
    }
}
