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
package com.indra.minsait.dvsmart.reorganization.domain.model;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

/**
 * Author: hahuaranga@indracompany.com
 * Created on: 12-12-2025 at 12:27:53
 * File: ProcessedArchivo.java
 */

@Data
@Builder
public class ProcessedArchivo {
    private String idUnico;
    private String rutaOrigen;
    private String rutaDestino;
    private String nombre;
    private String status;
    private Instant processedAt;
    private String errorMessage;
}
