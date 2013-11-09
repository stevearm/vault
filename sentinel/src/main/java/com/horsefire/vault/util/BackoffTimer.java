package com.horsefire.vault.util;

import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This works mostly like a normal {@link java.util.Timer}, except when the task
 * doesn't run properly, it'll be rerun immmediately (with a backoff for
 * multiple failures)
 */
public class BackoffTimer {

	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory
			.getLogger(BackoffTimer.class);

	private final Timer m_timer;

	public BackoffTimer(boolean isDaemon) {
		m_timer = new Timer(isDaemon);
	}

	public void start(BackoffTimerTask task, long periodMs) {
		m_timer.schedule(new InnerTimerTask(m_timer, task, periodMs), 1);
	}

	public void stop() {
		m_timer.cancel();
	}

	static class InnerTimerTask extends TimerTask {

		private final Timer m_timer;
		private final BackoffTimerTask m_task;
		private final long m_periodMs;
		private int m_backoffSeconds = 1;

		InnerTimerTask(Timer timer, BackoffTimerTask task, long periodMs) {
			m_timer = timer;
			m_task = task;
			m_periodMs = periodMs;
		}

		@Override
		public void run() {
			if (m_task.run()) {
				m_timer.schedule(this, m_periodMs);
				m_backoffSeconds = 1;
			} else {
				m_timer.schedule(this, m_backoffSeconds);
				m_backoffSeconds *= 2;
			}
		}
	}
}
