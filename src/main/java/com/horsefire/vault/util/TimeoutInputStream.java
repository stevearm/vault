package com.horsefire.vault.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimeoutInputStream extends InputStream {

	private static final Logger LOG = LoggerFactory
			.getLogger(TimeoutInputStream.class);

	private final int m_timeout;

	private final BlockingQueue<Integer> m_buffer = new ArrayBlockingQueue<Integer>(
			16);
	private final Thread m_thread;

	public TimeoutInputStream(final InputStream delegate, int timeout) {
		m_timeout = timeout;

		m_thread = new Thread() {
			@Override
			public void run() {
				try {
					while (true) {
						int result = delegate.read();
						if (result == -1) {
							LOG.info("InputStream closed");
							break;
						}
						m_buffer.put(Integer.valueOf(result));
					}
				} catch (IOException e) {
					LOG.error(
							"Wrapped input stream threw exception. Treating as closed",
							e);
				} catch (InterruptedException e) {
					LOG.error(
							"Buffer threw exception during put. Treating input stream as closed",
							e);
				}
				while (true) {
					try {
						m_buffer.put(-1);
					} catch (InterruptedException e) {
						LOG.error("Buffer threw exception during -1 put. Trying again");
					}
				}
			}
		};
		m_thread.setDaemon(true);
		m_thread.start();
	}

	@Override
	public int read() throws IOException {
		long end = System.currentTimeMillis() + m_timeout;
		while (System.currentTimeMillis() < end) {
			Integer result = m_buffer.poll();
			if (result != null) {
				return result.intValue();
			}
			try {
				Thread.sleep(2);
			} catch (InterruptedException e) {
				throw new IOException(e);
			}
		}
		throw new IOException("Read timed out");
	}

	@Override
	public void close() throws IOException {
		m_thread.interrupt();
	}

	public boolean isStreamOpen() {
		Integer peek = m_buffer.peek();
		return peek == null || peek.intValue() != -1;
	}
}
