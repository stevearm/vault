package com.horsefire.vault.couch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.lightcouch.CouchDbClient;
import org.lightcouch.CouchDbException;
import org.lightcouch.NoDocumentException;
import org.lightcouch.Replication;
import org.lightcouch.ReplicationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.horsefire.vault.CmdArgs;
import com.horsefire.vault.CouchDbClientFactory;
import com.horsefire.vault.SimpleHttpClient;

/**
 * Service to sync to all reachable vaults
 */
public class SyncService implements Runnable {

	private static final Logger LOG = LoggerFactory
			.getLogger(SyncService.class);

	private final CouchDbClientFactory m_factory;
	private final String m_id;
	private final String m_vaultName;
	private final String m_vaultDbName;
	private final SimpleHttpClient m_simpleClient;

	@Inject
	public SyncService(CouchDbClientFactory factory, CmdArgs cmdArgs,
			SimpleHttpClient simpleClient) {
		m_factory = factory;
		m_id = cmdArgs.id;
		m_vaultName = cmdArgs.dbVault;
		m_vaultDbName = cmdArgs.dbVaultDb;
		m_simpleClient = simpleClient;
	}

	public void run() {
		LOG.info("Starting sync");
		CouchDbClient client = m_factory.get(m_vaultDbName);
		try {
			VaultDocument doc = client.find(VaultDocument.class, m_id);

			List<VaultDocument> remoteVaults = getRemoteVaults(client);
			LOG.debug("Found {} remote vaults", remoteVaults.size());
			sortPriorityDesc(remoteVaults);

			for (VaultDocument syncTarget : remoteVaults) {
				sync(client, doc.dbs, syncTarget);
			}

			client.shutdown();
			LOG.info("Finished sync");
		} catch (NoDocumentException e) {
			LOG.error("Error downloading document vault/{}", m_id, e);
		}
	}

	static void sortPriorityDesc(List<VaultDocument> vaults) {
		Collections.sort(vaults, new Comparator<VaultDocument>() {
			public int compare(VaultDocument o1, VaultDocument o2) {
				int p1 = o1.addressable == null ? 0 : o1.addressable.priority;
				int p2 = o2.addressable == null ? 0 : o2.addressable.priority;
				return p1 == p2 ? 0 : (p1 < p2 ? 1 : -1);
			}
		});
	}

	private List<VaultDocument> getRemoteVaults(CouchDbClient client) {
		List<VaultDocument> vaults = new ArrayList<VaultDocument>();

		List<VaultDocument> possibleVaults = client.view("indexes/type")
				.key(VaultDocument.TYPE).includeDocs(Boolean.TRUE)
				.query(VaultDocument.class);
		for (VaultDocument doc : possibleVaults) {
			if (doc.addressable == null || !doc.addressable.enabled
					|| doc._id.equals(m_id)) {
				LOG.trace(
						"Skipping {} ({}) because it's not addressable, is disabled, or is me",
						doc.name, doc._id);
				continue;
			}
			vaults.add(doc);
		}
		return vaults;
	}

	private void sync(CouchDbClient couchClient, List<String> dbs,
			VaultDocument target) {
		String host = target.addressable.host;
		Integer port = target.addressable.port;

		LOG.debug("Sync to {} ({}) at {}:{}", target.name, target._id, host,
				port);

		try {
			JsonObject targetSignature = m_simpleClient.get(host, port, "/");
			if (!target.signature.equals(targetSignature)) {
				LOG.warn(
						"Remote vault {} ({}) does not match signature. Skipping",
						target.name, target._id);
				return;
			}
		} catch (IOException e) {
			LOG.warn(
					"Exception checking signature for remote vault {} ({}). Skipping",
					target.name, target._id, e);
			return;
		}

		sync(couchClient, target, m_vaultName, new String[] { "_design/ui" });
		sync(couchClient, target, m_vaultDbName, null);

		for (String db : dbs) {
			if (!target.dbs.contains(db)) {
				continue;
			}
			sync(couchClient, target, db, null);
		}
	}

	private void sync(CouchDbClient client, VaultDocument vault, String db,
			String[] docIds) {
		String remote = "http://" + vault.username + ":" + vault.password + "@"
				+ vault.addressable.host + ":" + vault.addressable.port + "/"
				+ db;
		try {
			push(client, remote, db, docIds);
		} catch (CouchDbException e) {
			LOG.error("Error pulling {} from {}", db, vault.name, e);
		}
		try {
			push(client, db, remote, docIds);
		} catch (CouchDbException e) {
			LOG.error("Error pushing {} to {}", db, vault.name, e);
		}
	}

	private void push(CouchDbClient client, String from, String to,
			String[] docIds) {
		LOG.debug("Replicating {} -> {}", from, to);
		Replication replication = client.replication().source(from).target(to)
				.createTarget(true);
		if (docIds != null) {
			replication.docIds(docIds);
		}
		ReplicationResult result = replication.trigger();
		if (!result.isOk()) {
			LOG.warn("Something went wrong during sync from {} to {}", from, to);
		}
	}
}
