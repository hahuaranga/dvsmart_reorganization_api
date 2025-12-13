package com.indra.minsait.dvsmart.reorganization.application.port.out;

import java.io.InputStream;

/**
 * Author: hahuaranga@indracompany.com
 * Created on: 12-12-2025 at 12:32:57
 * File: SftpOriginRepository.java
 */

public interface SftpOriginRepository {
	
    InputStream readFile(String path);

}
