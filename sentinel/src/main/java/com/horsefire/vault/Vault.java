package com.horsefire.vault;

import java.io.IOException;

import org.lightcouch.CouchDbClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.horsefire.vault.TaskLoop.Task;
import com.horsefire.vault.couch.SyncService;
import com.horsefire.vault.couch.VaultDocument;

public class Vault {

	private static final Logger LOG = LoggerFactory.getLogger(Vault.class);

	private final CouchDbClientFactory m_factory;
	private final String m_dbHost;
	private final Integer m_dbPort;
	private final String m_id;
	private final SimpleHttpClient m_simpleClient;
	private final SyncService m_syncService;
	private final TaskLoop m_taskLoop;

	@Inject
	public Vault(CouchDbClientFactory factory, @Named("dbHost") String dbHost,
			@Named("dbPort") Integer dbPort, @Named("id") String id,
			SimpleHttpClient simpleClient, SyncService syncService,
			TaskLoop taskLoop) {
		m_factory = factory;
		m_dbHost = dbHost;
		m_dbPort = dbPort;
		m_id = id;
		m_simpleClient = simpleClient;
		m_syncService = syncService;
		m_taskLoop = taskLoop;
	}

	public void run() {
		LOG.info("Starting sentinel");

		m_taskLoop.addTask(new Task() {
			public long periodMs() {
				return 24 * 60 * 60 * 1000;
			}

			public void run() {
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
					client.shutdown();
				} catch (IOException e) {
					LOG.warn("Exception checking signature", e);
				}
			}
		});

		m_taskLoop.addTask(new Task() {
			public long periodMs() {
				return 60 * 60 * 1000;
			}

			public void run() {
				m_syncService.sync();
			}
		});

		m_taskLoop.run();
	}
}
