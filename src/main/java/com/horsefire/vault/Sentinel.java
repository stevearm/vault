package com.horsefire.vault;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

public class Sentinel {

	public static interface Factory {
		Sentinel create(@Assisted("host") String host,
				@Assisted("id") String id, Quitter quitter);
	}

	private static final Logger LOG = LoggerFactory.getLogger(Sentinel.class);

	private final Options m_options;
	private final String m_host;
	private final String m_id;
	private final Quitter m_quitter;

	@Inject
	public Sentinel(Options options, @Assisted("host") String host,
			@Assisted("id") String id, @Assisted Quitter quitter) {
		m_options = options;
		m_host = host;
		m_id = id;
		m_quitter = quitter;
	}

	public void run() {
		LOG.warn("Starting with " + m_host + " and " + m_id);
		while (!m_quitter.shouldQuit()) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			LOG.warn("Still running");
		}
		LOG.info("Shutting down");
	}
}
