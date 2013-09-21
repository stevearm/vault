package com.horsefire.vault;

import java.io.IOException;

import org.lightcouch.CouchDbClient;
import org.lightcouch.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.horsefire.vault.VaultDocument.SyncTarget;

public class Sentinel {

	private static final Logger LOG = LoggerFactory
			.getLogger(SentinelTest.class);
	private static final long SLEEP_TIME_MS = 1000;

	private final CouchDbClientFactory m_factory;
	private final String m_id;
	private final Quitter m_quitter;
	private final SimpleHttpClient m_simpleClient;

	@Inject
	public Sentinel(CouchDbClientFactory factory, @Named("id") String id,
			Quitter quitter, SimpleHttpClient simpleClient) {
		m_factory = factory;
		m_id = id;
		m_quitter = quitter;
		m_simpleClient = simpleClient;
	}

	public void run() throws IOException {
		LOG.info("Starting sentinel");
		JsonObject signature = m_simpleClient.get("/");
		while (!m_quitter.shouldQuit()) {
			CouchDbClient client = m_factory.get("vault");
			VaultDocument doc = getDoc(client, signature);

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
		String expectedSignature = doc.signature.toString();
		String currentSignature = signature.toString();
		if (!currentSignature.equals(expectedSignature)) {
			LOG.info("Expected signature of {} but found {}. Saving new one",
					expectedSignature, currentSignature);
			doc.signature = signature;
			Response update = client.update(doc);
			doc._rev = update.getRev();
		}
		return doc;
	}

	private void sync(CouchDbClient client, SyncTarget target) {
		LOG.debug("Sync to {}", new Gson().toJson(target));
	}
}
