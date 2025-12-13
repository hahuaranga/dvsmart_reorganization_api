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
package com.indra.minsait.dvsmart.reorganization.adapter.in.web;

import com.indra.minsait.dvsmart.reorganization.application.port.in.StartReorganizeFullUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

/**
 * Author: hahuaranga@indracompany.com
 * Created on: 12-12-2025 at 13:25:04
 * File: BatchReorganizeController.java
 */

@Slf4j
@RestController
@RequestMapping("/api/batch/reorganize")
@RequiredArgsConstructor
public class BatchReorganizeController {

    private final StartReorganizeFullUseCase startReorganizeFullUseCase;

    @PostMapping("/full")
    public ResponseEntity<Map<String, Object>> startFullReorganization() {
        log.info("Received request to start full reorganization");
        
        Long jobExecutionId = startReorganizeFullUseCase.execute();
        
        return ResponseEntity.accepted()
                .body(Map.of(
                    "message", "Batch job started successfully",
                    "jobExecutionId", jobExecutionId,
                    "status", "ACCEPTED"
                ));
    }
}