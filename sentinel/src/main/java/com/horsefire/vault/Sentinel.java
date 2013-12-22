package com.horsefire.vault;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.horsefire.vault.couch.LocalDataService;
import com.horsefire.vault.couch.SyncService;

public class Sentinel {

	private static final Logger LOG = LoggerFactory.getLogger(Sentinel.class);

	private final LocalDataService m_localDataService;
	private final SyncService m_syncService;
	private final TaskLoop m_taskLoop;

	@Inject
	public Sentinel(LocalDataService localDataService, SyncService syncService,
			TaskLoop taskLoop) {
		m_localDataService = localDataService;
		m_syncService = syncService;
		m_taskLoop = taskLoop;
	}

	public void run() {
		LOG.info("Starting sentinel");

		m_taskLoop.addTask(m_localDataService, 60 * 60 * 1000);
		m_taskLoop.addTask(m_syncService, 60 * 60 * 1000);

		m_taskLoop.run();
	}
}
