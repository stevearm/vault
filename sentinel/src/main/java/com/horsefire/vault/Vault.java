package com.horsefire.vault;

import java.io.IOException;

import org.lightcouch.CouchDbClient;
import org.lightcouch.ReplicationResult;
import org.lightcouch.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.horsefire.vault.couch.VaultDocument;
import com.horsefire.vault.couch.VaultDocument.DbTarget;
import com.horsefire.vault.couch.VaultDocument.SyncTarget;

public class Vault {

	private static final Logger LOG = LoggerFactory.getLogger(Vault.class);
	private static final long SLEEP_TIME_MS = 1000;

	private final CouchDbClientFactory m_factory;
	private final String m_dbHost;
	private final Integer m_dbPort;
	private final String m_id;
	private final Quitter m_quitter;
	private final SimpleHttpClient m_simpleClient;

	@Inject
	public Vault(CouchDbClientFactory factory,
			@Named("dbHost") String dbHost, @Named("dbPort") Integer dbPort,
			@Named("id") String id, Quitter quitter,
			SimpleHttpClient simpleClient) {
		m_factory = factory;
		m_dbHost = dbHost;
		m_dbPort = dbPort;
		m_id = id;
		m_quitter = quitter;
		m_simpleClient = simpleClient;
	}

	public void run() throws IOException {
		LOG.info("Starting sentinel");
		long lastSync = 0;
		long syncFrequency = 0;
		while (!m_quitter.shouldQuit()) {
			long timestamp = System.currentTimeMillis();
			if ((lastSync + syncFrequency) < timestamp) {
				LOG.info("Starting sync");
				lastSync = timestamp;

				try {
					CouchDbClient client = m_factory.get("vault");
					JsonObject signature = m_simpleClient.get(m_dbHost,
							m_dbPort, "/");
					VaultDocument doc = getDoc(client, signature);
					syncFrequency = doc.sync_frequency_seconds * 1000;

					LOG.debug("Syncing to {} remote vaults", doc.sync.size());
					for (SyncTarget syncTarget : doc.sync) {
						// sync(client, syncTarget);
					}

					client.shutdown();
					LOG.info("Finished sync");
				} catch (IOException e) {
					LOG.warn("Exception during sync. Skipping", e);
				}
			}

			try {
				Thread.sleep(SLEEP_TIME_MS);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		LOG.info("Shutting down sentinel");
	}

	private VaultDocument getDoc(CouchDbClient client, JsonObject signature) {
		VaultDocument doc = client.find(VaultDocument.class, m_id);
		if (!signature.equals(doc.signature)) {
			LOG.info("Expected signature of {} but found {}. Saving new one",
					signature, doc.signature);
			doc.signature = signature;
			Response update = client.update(doc);
			doc._rev = update.getRev();
		}
		return doc;
	}

	private void sync(CouchDbClient couchClient, SyncTarget target)
			throws IOException {
		VaultDocument targetVault = couchClient.find(VaultDocument.class,
				target.id);
		String host = targetVault.host;
		Integer port = targetVault.port;

		LOG.debug("Sync to vault {} - {}:{}", new Object[] { targetVault._id,
				host, port });
		JsonObject targetSignature = m_simpleClient.get(host, port, "/");

		if (!targetVault.signature.equals(targetSignature)) {
			LOG.warn("Remote vault {} ({}) does not match signature. Skipping",
					targetVault.name, target.id);
			return;
		}

		for (DbTarget dbTarget : target.dbs) {
			String remote = buildUrl(targetVault, dbTarget.remote);

			switch (dbTarget.direction) {
			case BOTH:
				sync(couchClient, remote, dbTarget.local);
			case PUSH:
				sync(couchClient, dbTarget.local, remote);
				break;
			case PULL:
				sync(couchClient, remote, dbTarget.local);
				break;
			}
		}
	}

	private String buildUrl(VaultDocument vault, String db) {
		String credentials = "";
		if (vault.username != null) {
			credentials = vault.username + ":" + vault.password + "@";
		}
		return "http://" + credentials + vault.host + ":" + vault.port + "/"
				+ db;
	}

	private void sync(CouchDbClient client, String from, String to) {
		LOG.trace("Replicating {} -> {}", from, to);
		ReplicationResult result = client.replication().source(from).target(to)
				.trigger();
		if (!result.isOk()) {
			LOG.warn("Something went wrong during sync from {} to {}", from, to);
		}
	}
}
