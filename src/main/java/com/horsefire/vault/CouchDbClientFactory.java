package com.horsefire.vault;

import org.lightcouch.CouchDbClient;
import org.lightcouch.CouchDbProperties;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

public class CouchDbClientFactory implements Provider<CouchDbClient> {

	private final String m_dbHost;
	private final int m_dbPort;

	@Inject
	public CouchDbClientFactory(@Named("dbHost") String dbHost,
			@Named("dbPort") Integer dbPort) {
		m_dbHost = dbHost;
		m_dbPort = dbPort.intValue();
	}

	public CouchDbClient get() {
		CouchDbProperties properties = new CouchDbProperties()
				.setDbName("vault").setCreateDbIfNotExist(false)
				.setProtocol("http").setHost(m_dbHost).setPort(m_dbPort)
				.setMaxConnections(5).setConnectionTimeout(500);
		return new CouchDbClient(properties);
	}
}
