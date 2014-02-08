package com.horsefire.vault.couch;

import java.io.IOException;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.lightcouch.CouchDbClient;
import org.lightcouch.NoDocumentException;
import org.lightcouch.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.horsefire.vault.CmdArgs;
import com.horsefire.vault.CouchDbClientFactory;
import com.horsefire.vault.SimpleHttpClient;
import com.horsefire.vault.Vault;

public class LocalDataService implements Runnable {

	private static final Logger LOG = LoggerFactory
			.getLogger(LocalDataService.class);

	private static final String VAULT_RELEASE = "http://stevearm.iriscouch.com/vault-release";
	private static final String VAULTDB_RELEASE = "http://stevearm.iriscouch.com/vaultdb-release";

	private final CouchDbClientFactory m_factory;
	private final String m_dbHost;
	private final Integer m_dbPort;
	private final String m_dbUsername;
	private final String m_dbPassword;
	private final String m_id;
	private final String m_vaultName;
	private final String m_vaultDbName;
	private final SimpleHttpClient m_simpleClient;

	@Inject
	public LocalDataService(CouchDbClientFactory factory, CmdArgs cmdArgs,
			SimpleHttpClient simpleClient) {
		m_factory = factory;
		m_dbHost = cmdArgs.dbHost;
		m_dbPort = cmdArgs.dbPort;
		m_dbUsername = cmdArgs.dbUsername;
		m_dbPassword = cmdArgs.dbPassword;
		m_id = cmdArgs.id;
		m_vaultName = cmdArgs.dbVault;
		m_vaultDbName = cmdArgs.dbVaultDb;
		m_simpleClient = simpleClient;
	}

	public void run() {
		checkPublicDb();
		checkPrivateDb();
	}

	private void checkPublicDb() {
		CouchDbClient client = m_factory.get(m_vaultName);

		if (!client.context().getAllDbs().contains(m_vaultName)) {
			LOG.info("Bootstrapping {} db from {}", m_vaultName, VAULT_RELEASE);
			client.replication().source(VAULT_RELEASE).target(m_vaultName)
					.createTarget(true).trigger();
		}

		IdDocument doc;
		try {
			doc = client.find(IdDocument.class, IdDocument.ID);
		} catch (NoDocumentException e) {
			doc = new IdDocument();
			doc._id = IdDocument.ID;
			LOG.info("Creating {} document in {}", doc._id, m_vaultName);
		}

		// Tag this run
		doc.sentinelRun = new DateTime().toString(ISODateTimeFormat
				.dateTimeNoMillis());

		if (!m_vaultDbName.equals(doc.vaultDbName)) {
			doc.vaultDbName = m_vaultDbName;
			LOG.info("Updating vaultDbName in {}/{}", m_vaultName, doc._id);
		}

		if (!m_id.equals(doc.vaultId)) {
			doc.vaultId = m_id;
			LOG.info("Updating vaultId in {}/{}", m_vaultName, doc._id);
		}

		if (doc._rev == null) {
			Response save = client.save(doc);
			if (save.getError() != null) {
				LOG.error("Could not create {} in {} db: {} {}", doc._id,
						m_vaultName, save.getError(), save.getReason());
			}
		} else {
			Response save = client.update(doc);
			if (save.getError() != null) {
				LOG.error("Could not update {} in {} db: {} {}", doc._id,
						m_vaultName, save.getError(), save.getReason());
			}
		}
		client.shutdown();

		try {
			m_simpleClient.get(m_dbHost, m_dbPort, "/" + m_vaultName + "/"
					+ IdDocument.ID);
		} catch (IOException e) {
			LOG.error("Should be able to publicly read {} db", m_vaultName, e);
		}
	}

	private void checkPrivateDb() {
		CouchDbClient client = m_factory.get(m_vaultDbName);

		if (!client.context().getAllDbs().contains(m_vaultDbName)) {
			LOG.info("Bootstrapping {} db from {}", m_vaultDbName,
					VAULTDB_RELEASE);
			client.replication().source(VAULTDB_RELEASE).target(m_vaultDbName)
					.createTarget(true).trigger();
		}

		boolean dirty = false;
		VaultDocument doc;
		try {
			doc = client.find(VaultDocument.class, m_id);
		} catch (NoDocumentException e) {
			doc = new VaultDocument();
			doc._id = m_id;
			doc.name = "New vault (" + m_id + ")";
			LOG.info("Creating {} document in {}", doc._id, m_vaultDbName);
			dirty = true;
		}

		try {
			JsonObject signature = m_simpleClient.get(m_dbHost, m_dbPort, "/");
			if (!signature.equals(doc.signature)) {
				LOG.info(
						"Expected signature of {} but found {}. Saving new one",
						signature, doc.signature);
				doc.signature = signature;
				dirty = true;
			}
		} catch (IOException e) {
			LOG.warn("Exception checking signature", e);
		}

		if (!m_dbUsername.equals(doc.username)) {
			doc.username = m_dbUsername;
			LOG.info("Updating username in {}/{}", m_vaultDbName, doc._id);
			dirty = true;
		}
		if (!m_dbPassword.equals(doc.password)) {
			doc.password = m_dbPassword;
			LOG.info("Updating password in {}/{}", m_vaultDbName, doc._id);
			dirty = true;
		}
		if (!Vault.VERSION.equals(doc.sentinel)) {
			doc.sentinel = Vault.VERSION;
			LOG.info("Updating sentinel version in {}/{}", m_vaultDbName,
					doc._id);
			dirty = true;
		}

		if (dirty) {
			if (doc._rev == null) {
				Response save = client.save(doc);
				if (save.getError() != null) {
					LOG.error("Could not create {} in {} db: {} {}", doc._id,
							m_vaultDbName, save.getError(), save.getReason());
				}
			} else {
				Response save = client.update(doc);
				if (save.getError() != null) {
					LOG.error("Could not update {} in {} db: {} {}", doc._id,
							m_vaultDbName, save.getError(), save.getReason());
				}
			}
		}
		client.shutdown();

		try {
			LOG.info("Doing unauthenticated GET to test security. Expecting 401");
			m_simpleClient.get(m_dbHost, m_dbPort, "/" + m_vaultDbName + "/"
					+ m_id);
			LOG.error("Should not be able to publicly read {} db",
					m_vaultDbName);
		} catch (IOException e) {
		}
	}
}
