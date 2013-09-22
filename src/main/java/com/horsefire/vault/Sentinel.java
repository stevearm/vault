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
import com.horsefire.vault.VaultDocument.DbTarget;
import com.horsefire.vault.VaultDocument.SyncTarget;

public class Sentinel {

	private static final Logger LOG = LoggerFactory.getLogger(Sentinel.class);
	private static final long SLEEP_TIME_MS = 1000;

	private final CouchDbClientFactory m_factory;
	private final String m_dbHost;
	private final Integer m_dbPort;
	private final String m_id;
	private final Quitter m_quitter;
	private final SimpleHttpClient m_simpleClient;

	@Inject
	public Sentinel(CouchDbClientFactory factory,
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
		while (!m_quitter.shouldQuit()) {
			CouchDbClient client = m_factory.get("vault");

			JsonObject signature = m_simpleClient.get(m_dbHost, m_dbPort, "/");
			VaultDocument doc = getDoc(client, signature);

			LOG.debug("Syncing to {} remote vaults", doc.sync.size());
			for (SyncTarget syncTarget : doc.sync) {
				sync(client, syncTarget);
			}

			client.shutdown();

			try {
				Thread.sleep(SLEEP_TIME_MS);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			break;
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

		String targetUrlBase = "http://" + host + ":" + port + "/";
		sync(couchClient, "vault", targetUrlBase + "vault");
		sync(couchClient, targetUrlBase + "vault", "vault");
		for (DbTarget dbTarget : target.dbs) {
			switch (dbTarget.direction) {
			case BOTH:
				sync(couchClient, targetUrlBase + dbTarget.remote,
						dbTarget.local);
			case PUSH:
				sync(couchClient, dbTarget.local, targetUrlBase
						+ dbTarget.remote);
				break;
			case PULL:
				sync(couchClient, targetUrlBase + dbTarget.remote,
						dbTarget.local);
				break;
			}
		}
	}

	private void sync(CouchDbClient client, String from, String to) {
		LOG.trace("Replicating {} -> {}", from, to);
		ReplicationResult result = client.replication().source(from).target(to)
				.createTarget(true).trigger();
		if (!result.isOk()) {
			LOG.warn("Something went wrong during sync from {} to {}", from, to);
		}
	}
}
