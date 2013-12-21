package com.horsefire.vault;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.horsefire.vault.couch.LocalDataService;
import com.horsefire.vault.couch.SyncService;
import com.horsefire.vault.worker.WorkerService;

public class Sentinel {

	private static final Logger LOG = LoggerFactory.getLogger(Sentinel.class);

	private final CouchDbClientFactory m_factory;
	private final String m_dbHost;
	private final Integer m_dbPort;
	private final String m_id;
	private final SimpleHttpClient m_simpleClient;
	private final LocalDataService m_localDataService;
	private final SyncService m_syncService;
	private final TaskLoop m_taskLoop;
	private final WorkerService m_workerService;

	@Inject
	public Sentinel(CouchDbClientFactory factory,
			@Named("dbHost") String dbHost, @Named("dbPort") Integer dbPort,
			@Named("id") String id, SimpleHttpClient simpleClient,
			LocalDataService localDataService, SyncService syncService,
			TaskLoop taskLoop, WorkerService workerService) {
		m_factory = factory;
		m_dbHost = dbHost;
		m_dbPort = dbPort;
		m_id = id;
		m_simpleClient = simpleClient;
		m_localDataService = localDataService;
		m_syncService = syncService;
		m_taskLoop = taskLoop;
		m_workerService = workerService;
	}

	public void run() {
		LOG.info("Starting sentinel");

		m_taskLoop.addTask(m_localDataService, 60 * 60 * 1000);

		// m_taskLoop.addTask(m_syncService, 60 * 60 * 1000);
		// m_taskLoop.addTask(m_workerService, 5 * 60 * 1000);

		m_taskLoop.run();
	}
}
