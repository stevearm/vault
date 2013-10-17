package com.horsefire.vault;

import java.io.IOException;

import org.lightcouch.CouchDbClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.horsefire.vault.couch.SyncService;
import com.horsefire.vault.couch.VaultDocument;

public class Vault {

	private static final Logger LOG = LoggerFactory.getLogger(Vault.class);

	private static final long SLEEP_TIME_MS = 1000;
	private static final long SIG_CHECK_PERIOD_MS = 24 * 60 * 60 * 1000;
	private static final long SYNC_PERIOD_MS = 60 * 60 * 1000;

	private final CouchDbClientFactory m_factory;
	private final String m_dbHost;
	private final Integer m_dbPort;
	private final String m_id;
	private final Quitter m_quitter;
	private final SimpleHttpClient m_simpleClient;
	private final SyncService m_syncService;

	@Inject
	public Vault(CouchDbClientFactory factory, @Named("dbHost") String dbHost,
			@Named("dbPort") Integer dbPort, @Named("id") String id,
			Quitter quitter, SimpleHttpClient simpleClient,
			SyncService syncService) {
		m_factory = factory;
		m_dbHost = dbHost;
		m_dbPort = dbPort;
		m_id = id;
		m_quitter = quitter;
		m_simpleClient = simpleClient;
		m_syncService = syncService;
	}

	public void run() {
		LOG.info("Starting sentinel");

		long nextSigCheck = 0;
		long nextSync = 0;
		while (!m_quitter.shouldQuit()) {
			long now = System.currentTimeMillis();
			if (nextSigCheck < now) {
				LOG.info("Checking signature");
				try {
					JsonObject signature = m_simpleClient.get(m_dbHost,
							m_dbPort, "/");
					CouchDbClient client = m_factory.get("vault");
					VaultDocument doc = client.find(VaultDocument.class, m_id);
					if (!signature.equals(doc.signature)) {
						LOG.info(
								"Expected signature of {} but found {}. Saving new one",
								signature, doc.signature);
						doc.signature = signature;
						client.update(doc);
					}
					nextSigCheck = now + SIG_CHECK_PERIOD_MS;
					client.shutdown();
				} catch (IOException e) {
					LOG.warn("Exception checking signature", e);
				}
			}

			if (nextSync < now) {
				m_syncService.sync();
				nextSync = now + SYNC_PERIOD_MS;
			}

			try {
				Thread.sleep(SLEEP_TIME_MS);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		LOG.info("Shutting down sentinel");
	}
}
