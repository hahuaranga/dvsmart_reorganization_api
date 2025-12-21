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
package com.indra.minsait.dvsmart.reorganization.application.service;

import com.indra.minsait.dvsmart.reorganization.application.port.in.StartReorganizeFullUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.stereotype.Service;

/**
 * Author: hahuaranga@indracompany.com
 * Created on: 12-12-2025 at 12:54:15
 * File: StartReorganizeFullService.java
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class StartReorganizeFullService implements StartReorganizeFullUseCase {

    private final JobOperator jobOperator;

    private final Job batchReorgFullJob;
    
    @Override
    @SchedulerLock(
            name = "reorganize-full-job",
            lockAtMostFor = "PT2H",    // 2 horas máximo
            lockAtLeastFor = "PT30M"   // 30 minutos mínimo
        )    
    public Long execute() {
        try {
            // ✅ JobParametersBuilder en lugar de Properties
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .addString("uniqueId", java.util.UUID.randomUUID().toString())
                    .toJobParameters();  
            
            // Lanzar job usando el nombre del job (definido en BatchReorgFullConfig)
            JobExecution jobExecution = jobOperator.start(batchReorgFullJob, jobParameters);
            
            log.info("Job launched successfully. JobExecutionId: {}, Status: {}", 
                    jobExecution.getId(), jobExecution.getStatus());
            
            return jobExecution.getId();
            
        } catch (Exception e) {
            log.error("Failed to launch batch job", e);
            throw new RuntimeException("Failed to start reorganization job", e);
        }
    }
}