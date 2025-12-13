package com.indra.minsait.dvsmart.reorganization.infrastructure.sftp;

import com.indra.minsait.dvsmart.reorganization.infrastructure.config.SftpConfigProperties;
import lombok.RequiredArgsConstructor;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;

/**
 * Author: hahuaranga@indracompany.com
 * Created on: 12-12-2025 at 13:05:17
 * File: SftpSessionFactoryConfig.java
 */

@Configuration
@RequiredArgsConstructor
public class SftpSessionFactoryConfig {

	private final SftpConfigProperties props;

	@Bean(name = "sftpOriginSessionFactory")
	SessionFactory<RemoteResourceInfo> sftpOriginSessionFactory() {
		DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory(true);
		factory.setHost(props.getOrigin().getHost());
		factory.setPort(props.getOrigin().getPort());
		factory.setUser(props.getOrigin().getUser());
		factory.setPassword(props.getOrigin().getPassword());
		factory.setTimeout(props.getOrigin().getTimeout());
		factory.setAllowUnknownKeys(true);

		CachingSessionFactory<RemoteResourceInfo> cachingFactory = new CachingSessionFactory<>(factory);
		cachingFactory.setPoolSize(props.getOrigin().getPool().getSize());

		return cachingFactory;
	}

	@Bean(name = "sftpDestinationSessionFactory")
	SessionFactory<RemoteResourceInfo> sftpDestinationSessionFactory() {
		DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory(true);
		factory.setHost(props.getDest().getHost());
		factory.setPort(props.getDest().getPort());
		factory.setUser(props.getDest().getUser());
		factory.setPassword(props.getDest().getPassword());
		factory.setTimeout(props.getDest().getTimeout());
		factory.setAllowUnknownKeys(true);

		CachingSessionFactory<RemoteResourceInfo> cachingFactory = new CachingSessionFactory<>(factory);
		cachingFactory.setPoolSize(props.getDest().getPool().getSize());

		return cachingFactory;
	}

	@Bean(name = "sftpOriginTemplate")
	SftpRemoteFileTemplate sftpOriginTemplate() {
		return new SftpRemoteFileTemplate(sftpOriginSessionFactory());
	}

	@Bean(name = "sftpDestinationTemplate")
	SftpRemoteFileTemplate sftpDestinationTemplate() {
		return new SftpRemoteFileTemplate(sftpDestinationSessionFactory());
	}
}