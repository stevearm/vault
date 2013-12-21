package com.horsefire.vault.worker;

import java.io.IOException;
import java.util.List;

import org.joda.time.DateTime;
import org.lightcouch.CouchDbClient;
import org.lightcouch.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.horsefire.vault.CouchDbClientFactory;
import com.horsefire.vault.couch.AppDocument;
import com.horsefire.vault.couch.WorkerDocument;

/**
 * This class will poll all worker's db entries, checks the triggered timestamp
 * to see if it's after the started timestamp. If so, it runs the worker, and
 * sets started to now. When the worker is finished, it sets finished to now.
 */
public class WorkerService implements Runnable {

	private static final Logger LOG = LoggerFactory
			.getLogger(WorkerService.class);

	private final CouchDbClientFactory m_clientFactory;
	private final WorkerExecutionService m_execService;

	@Inject
	public WorkerService(CouchDbClientFactory clientFactory,
			WorkerExecutionService execService) {
		m_clientFactory = clientFactory;
		m_execService = execService;
	}

	public void run() {
		CouchDbClient couchClient = m_clientFactory.get("vault");
		List<AppDocument> apps = null;
		try {
			apps = couchClient.view("indexes/type").key(AppDocument.TYPE)
					.includeDocs(Boolean.TRUE).query(AppDocument.class);
		} finally {
			couchClient.shutdown();
		}
		for (AppDocument app : apps) {
			if (app.worker == null || app.worker.isEmpty()) {
				continue;
			}

			couchClient = m_clientFactory.get(app.database);
			try {
				WorkerDocument doc = couchClient.find(WorkerDocument.class,
						app.worker);
				if (doc.triggered.isAfter(doc.finished)) {
					LOG.debug("Triggering {}/{} worker", app.database, app.ui);
					doc.started = new DateTime();
					Response response = couchClient.update(doc);
					doc._rev = response.getRev();

					try {
						m_execService.runWorker(app.database, app.ui);
					} catch (IOException e) {
						LOG.warn("Error running worker", e);
					} catch (InterruptedException e) {
						LOG.warn("Error running worker", e);
					}

					doc.finished = new DateTime();
					couchClient.update(doc);
				}
			} finally {
				couchClient.shutdown();
				couchClient = null;
			}
		}
	}
}
