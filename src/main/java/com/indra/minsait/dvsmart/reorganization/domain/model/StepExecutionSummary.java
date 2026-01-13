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
package com.indra.minsait.dvsmart.reorganization.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Author: hahuaranga@indracompany.com
 * Created on: 28-12-2025 at 23:16:59
 * File: StepExecutionSummary.java
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepExecutionSummary {
    private String stepName;
    private String status;
    private Integer readCount;
    private Integer writeCount;
    private Integer skipCount;
    private String duration;
}
