package com.horsefire.vault;

import com.horsefire.vault.util.TimeoutInputStream;

public class Quitter {

	private final long m_time;
	private final TimeoutInputStream m_inputStream;

	public Quitter(long quittingTime) {
		m_time = quittingTime;
		m_inputStream = null;
	}

	public Quitter(long quittingTime, TimeoutInputStream inputStream) {
		m_time = quittingTime;
		m_inputStream = inputStream;
	}

	public boolean shouldQuit() {
		if (m_time < System.currentTimeMillis()) {
			return true;
		}
		if (m_inputStream != null) {
			return !m_inputStream.isStreamOpen();
		}
		return false;
	}
}
