package com.horsefire.vault.couch;

import java.io.IOException;

import org.lightcouch.CouchDbClient;
import org.lightcouch.NoDocumentException;
import org.lightcouch.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.horsefire.vault.CouchDbClientFactory;
import com.horsefire.vault.SimpleHttpClient;

public class LocalDataService implements Runnable {

	private static final Logger LOG = LoggerFactory
			.getLogger(LocalDataService.class);

	private final CouchDbClientFactory m_factory;
	private final String m_dbHost;
	private final Integer m_dbPort;
	private final String m_dbUsername;
	private final String m_dbPassword;
	private final String m_id;
	private final SimpleHttpClient m_simpleClient;

	@Inject
	public LocalDataService(CouchDbClientFactory factory,
			@Named("dbHost") String dbHost, @Named("dbPort") Integer dbPort,
			@Named("dbUsername") String dbUsername,
			@Named("dbPassword") String dbPassword, @Named("id") String id,
			SimpleHttpClient simpleClient) {
		m_factory = factory;
		m_dbHost = dbHost;
		m_dbPort = dbPort;
		m_dbUsername = dbUsername;
		m_dbPassword = dbPassword;
		m_id = id;
		m_simpleClient = simpleClient;
	}

	public void run() {
		checkPublicDb();
		checkPrivateDb();
	}

	private void checkPublicDb() {
		String dbName = "vault";
		CouchDbClient client = m_factory.get(dbName);

		if (!client.context().getAllDbs().contains(dbName)) {
			LOG.info("Creating {} db", dbName);
			client.context().createDB(dbName);
		}

		boolean dirty = false;
		IdDocument doc;
		try {
			doc = client.find(IdDocument.class, IdDocument.ID);
		} catch (NoDocumentException e) {
			doc = new IdDocument();
			doc._id = IdDocument.ID;
			LOG.info("Creating {} document in {}", doc._id, dbName);
			dirty = true;
		}
		if (!m_id.equals(doc.vaultId)) {
			doc.vaultId = m_id;
			LOG.info("Updating vaultId in {}/{}", dbName, doc._id);
			dirty = true;
		}

		if (dirty) {
			if (doc._rev == null) {
				Response save = client.save(doc);
				if (save.getError() != null) {
					LOG.error("Could not create {} in {} db: {} {}", doc._id,
							dbName, save.getError(), save.getReason());
				}
			} else {
				Response save = client.update(doc);
				if (save.getError() != null) {
					LOG.error("Could not update {} in {} db: {} {}", doc._id,
							dbName, save.getError(), save.getReason());
				}
			}
		}
		client.shutdown();

		try {
			m_simpleClient.get(m_dbHost, m_dbPort, "/" + dbName + "/"
					+ IdDocument.ID);
		} catch (IOException e) {
			LOG.error("Should be able to publicly read {} db", dbName, e);
		}
	}

	private void checkPrivateDb() {
		String dbName = "vaultdb";
		CouchDbClient client = m_factory.get(dbName);

		if (!client.context().getAllDbs().contains(dbName)) {
			LOG.info("Creating {} db", dbName);
			client.context().createDB(dbName);
		}

		boolean dirty = false;
		VaultDocument doc;
		try {
			doc = client.find(VaultDocument.class, m_id);
		} catch (NoDocumentException e) {
			doc = new VaultDocument();
			doc._id = m_id;
			doc.name = "New vault (" + m_id + ")";
			LOG.info("Creating {} document in {}", doc._id, dbName);
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
			LOG.info("Updating username in {}/{}", dbName, doc._id);
			dirty = true;
		}
		if (!m_dbPassword.equals(doc.password)) {
			doc.password = m_dbPassword;
			LOG.info("Updating password in {}/{}", dbName, doc._id);
			dirty = true;
		}

		if (dirty) {
			if (doc._rev == null) {
				Response save = client.save(doc);
				if (save.getError() != null) {
					LOG.error("Could not create {} in {} db: {} {}", doc._id,
							dbName, save.getError(), save.getReason());
				}
			} else {
				Response save = client.update(doc);
				if (save.getError() != null) {
					LOG.error("Could not update {} in {} db: {} {}", doc._id,
							dbName, save.getError(), save.getReason());
				}
			}
		}
		client.shutdown();

		try {
			m_simpleClient.get(m_dbHost, m_dbPort, "/" + dbName + "/" + m_id);
			LOG.error("Should not be able to publicly read {} db", dbName);
		} catch (IOException e) {
		}
	}
}
