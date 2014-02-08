package com.horsefire.vault;

import org.lightcouch.CouchDbClient;
import org.lightcouch.CouchDbProperties;

import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class CouchDbClientFactory {

	private final String m_dbHost;
	private final int m_dbPort;
	private final String m_dbUsername;
	private final String m_dbPassword;
	private final Provider<GsonBuilder> m_gsonBuilderProvider;

	@Inject
	public CouchDbClientFactory(CmdArgs cmdArgs,
			Provider<GsonBuilder> gsonBuilderProvider) {
		m_dbHost = cmdArgs.dbHost;
		m_dbPort = cmdArgs.dbPort;
		m_dbUsername = cmdArgs.dbUsername;
		m_dbPassword = cmdArgs.dbPassword;
		m_gsonBuilderProvider = gsonBuilderProvider;
	}

	public CouchDbClient get(String dbName) {
		CouchDbProperties properties = new CouchDbProperties()
				.setDbName(dbName).setCreateDbIfNotExist(false)
				.setProtocol("http").setHost(m_dbHost).setPort(m_dbPort)
				.setMaxConnections(5).setConnectionTimeout(500)
				.setUsername(m_dbUsername).setPassword(m_dbPassword);

		CouchDbClient client = new CouchDbClient(properties);
		client.setGsonBuilder(m_gsonBuilderProvider.get());
		return client;
	}
}
