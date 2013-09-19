package com.horsefire.vault;

import org.lightcouch.CouchDbClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class Sentinel {

	private static final Logger LOG = LoggerFactory.getLogger(Sentinel.class);
	private static final long SLEEP_TIME_MS = 1000;

	private final CouchDbClientFactory m_factory;
	private final String m_id;
	private final Quitter m_quitter;

	@Inject
	public Sentinel(CouchDbClientFactory factory, @Named("id") String id,
			Quitter quitter) {
		m_factory = factory;
		m_id = id;
		m_quitter = quitter;
	}

	private void tryGet() {
		CouchDbClient client = m_factory.get();
		JsonObject doc = client.find(JsonObject.class, m_id);
		LOG.debug("Got doc: {}", doc);
		client.shutdown();
	}

	public void run() {
		LOG.info("Starting sentinel");
		while (!m_quitter.shouldQuit()) {
			tryGet();

			try {
				Thread.sleep(SLEEP_TIME_MS);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			break;
		}
		LOG.info("Shutting down sentinel");
	}
}
