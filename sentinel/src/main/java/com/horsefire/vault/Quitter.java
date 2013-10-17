package com.horsefire.vault;

import java.util.concurrent.atomic.AtomicLong;

import com.horsefire.vault.util.TimeoutInputStream;

public class Quitter {

	private final long m_startTimeMillis;
	private final AtomicLong m_runtimeMillis;
	private final TimeoutInputStream m_inputStream;

	public Quitter(long runtimeMillis, TimeoutInputStream inputStream) {
		m_startTimeMillis = System.currentTimeMillis();
		m_runtimeMillis = new AtomicLong(runtimeMillis);
		m_inputStream = inputStream;
	}

	public void setRuntime(long runtime) {
		m_runtimeMillis.set(runtime);
	}

	public boolean shouldQuit() {
		if (m_inputStream != null && !m_inputStream.isStreamOpen()) {
			return true;
		}
		return ((m_startTimeMillis + m_runtimeMillis.get()) < System.currentTimeMillis());
	}
}
