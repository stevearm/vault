package com.horsefire.vault.couch;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.lightcouch.CouchDbClient;
import org.lightcouch.ReplicationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.horsefire.vault.CouchDbClientFactory;
import com.horsefire.vault.SimpleHttpClient;

/**
 * Periodically try to sync to all reachable vaults
 */
public class SyncService {

	private static final Logger LOG = LoggerFactory
			.getLogger(SyncService.class);

	private final CouchDbClientFactory m_factory;
	private final String m_id;
	private final SimpleHttpClient m_simpleClient;

	@Inject
	public SyncService(CouchDbClientFactory factory, @Named("id") String id,
			SimpleHttpClient simpleClient) {
		m_factory = factory;
		m_id = id;
		m_simpleClient = simpleClient;
	}

	public void run() throws IOException {
		LOG.info("Starting sync");
		try {
			CouchDbClient client = m_factory.get("vault");
			VaultDocument doc = client.find(VaultDocument.class, m_id);

			List<VaultDocument> remoteVaults = getTargetVaults(client, doc.dbs);
			LOG.debug("Syncing to {} remote vaults", remoteVaults.size());
			Collections.sort(remoteVaults, new Comparator<VaultDocument>() {
				public int compare(VaultDocument o1, VaultDocument o2) {
					return o1.priority == o2.priority ? 0
							: (o1.priority < o2.priority ? -1 : 1);
				}
			});

			for (VaultDocument syncTarget : remoteVaults) {
				sync(client, doc.dbs, syncTarget);
			}

			client.shutdown();
			LOG.info("Finished sync");
		} catch (IOException e) {
			LOG.warn("Exception during sync. Skipping", e);
		}
		LOG.info("Finished sync");
	}

	private List<VaultDocument> getTargetVaults(CouchDbClient client,
			List<String> dbs) {
		List<VaultDocument> vaults = client.view("type")
				.key(VaultDocument.TYPE).includeDocs(Boolean.TRUE)
				.query(VaultDocument.class);

		for (Iterator<VaultDocument> it = vaults.iterator(); it.hasNext();) {
			VaultDocument doc = it.next();
			if (!doc._id.equals(m_id) && doc.host != null
					&& !doc.host.isEmpty()) {
				for (String db : dbs) {
					if (doc.dbs.contains(db)) {
						continue;
					}
				}
			}
			it.remove();
		}
		return vaults;
	}

	private void sync(CouchDbClient couchClient, List<String> dbs,
			VaultDocument target) throws IOException {
		String host = target.host;
		Integer port = target.port;

		LOG.debug("Sync to vault {} - {}:{}", new Object[] { target._id, host,
				port });
		JsonObject targetSignature = m_simpleClient.get(host, port, "/");

		if (!target.signature.equals(targetSignature)) {
			LOG.warn("Remote vault {} ({}) does not match signature. Skipping",
					target.name, target._id);
			return;
		}

		for (String db : dbs) {
			if (!target.dbs.contains(db)) {
				continue;
			}

			String remote = buildUrl(target, db);

			sync(couchClient, remote, db);
			sync(couchClient, db, remote);
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
