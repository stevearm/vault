package com.horsefire.vault;

import com.google.gson.GsonBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

public class VaultModule extends AbstractModule {

	private final String m_dbHost;
	private final Integer m_dbPort;
	private final String m_dbPassword;
	private final String m_id;
	private final Boolean m_isDebug;
	private final Quitter m_quitter;

	public VaultModule(String host, int port, String password, String id,
			boolean isDebug, Quitter quitter) {
		m_dbHost = host;
		m_dbPort = Integer.valueOf(port);
		m_dbPassword = password;
		m_id = id;
		m_isDebug = Boolean.valueOf(isDebug);
		m_quitter = quitter;
	}

	private void bindMemberConstants() {
		bind(String.class).annotatedWith(Names.named("dbHost")).toInstance(
				m_dbHost);
		bind(Integer.class).annotatedWith(Names.named("dbPort")).toInstance(
				m_dbPort);
		bind(String.class).annotatedWith(Names.named("dbPassword")).toInstance(
				(m_dbPassword == null) ? "" : m_dbPassword);
		bind(String.class).annotatedWith(Names.named("id")).toInstance(m_id);
		bind(Boolean.class).annotatedWith(Names.named("isDebug")).toInstance(
				m_isDebug);
		bind(Quitter.class).toInstance(m_quitter);
	}

	@Override
	protected void configure() {
		bindMemberConstants();

		bind(GsonBuilder.class).toProvider(GsonBuilderProvider.class);
	}
}
