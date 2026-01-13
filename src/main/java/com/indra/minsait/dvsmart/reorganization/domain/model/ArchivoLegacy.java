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

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

/**
 * Author: hahuaranga@indracompany.com
 * Created on: 12-12-2025 at 12:26:43
 * File: ArchivoLegacy.java
 */

@Data
@Builder
public class ArchivoLegacy {
    private String idUnico;
    private String rutaOrigen;
    private String nombre;
    private Instant mtime;
}