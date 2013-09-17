package com.horsefire.vault;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.TestCase;

import org.junit.Test;

public class CouchDbCommunicatorTest extends TestCase {

	@Test
	public void testWorking() throws Exception {
		CouchDbMock mock = new CouchDbMock(
				"[\"get\", \"couchdb\", \"uuid\"]\n",
				"\"ad3baac30bcd3c62577e9ef239c38b9c\"", 50);

		CouchDbCommunicator communicator = new CouchDbCommunicator(
				new PrintStream(mock), mock.getInputStream());
		String id = communicator.getUuid();

		assertEquals("ad3baac30bcd3c62577e9ef239c38b9c", id);
	}

	@Test
	public void testMissingKey() throws Exception {
		CouchDbMock mock = new CouchDbMock(
				"[\"get\", \"couchdb\", \"uuid\"]\n", "null", 50);

		CouchDbCommunicator communicator = new CouchDbCommunicator(
				new PrintStream(mock), mock.getInputStream());
		String id = communicator.getUuid();

		assertNull(id);
	}

	@Test
	public void testBlockingForever() throws Exception {
		final AtomicBoolean threadRunning = new AtomicBoolean(true);
		final AtomicBoolean gotAnswer = new AtomicBoolean(false);
		Thread thread = new Thread() {
			@Override
			public void run() {
				try {
					CouchDbMock mock = new CouchDbMock(
							"[\"get\", \"couchdb\", \"uuid\"]\n", "something",
							10000);

					CouchDbCommunicator communicator = new CouchDbCommunicator(
							new PrintStream(mock), mock.getInputStream());
					communicator.getUuid();
					gotAnswer.set(true);
				} catch (IOException e) {
					System.err.println("Exception during test: " + e);
				}
				threadRunning.set(false);
			}
		};
		thread.start();

		Thread.sleep(5000);
		if (threadRunning.get()) {
			thread.interrupt();
			fail("Thread timeout failed, still blocked");
		}
		if (gotAnswer.get()) {
			fail("Should not have gotten an answer, though have thrown exception");
		}
	}

	private class CouchDbMock extends OutputStream {

		private final StringBuilder m_buffer = new StringBuilder();
		private final String m_expect;
		private final String m_response;
		private final int m_delayMs;

		private final BlockingQueue<Byte> m_queue;
		private final InputStream m_responseStream;

		public CouchDbMock(String expect, String response, int delayMs) {
			m_expect = expect;
			m_response = response;
			m_delayMs = delayMs;

			m_queue = new ArrayBlockingQueue<Byte>(response.length() + 2);
			m_responseStream = new InputStream() {
				@Override
				public int read() throws IOException {
					try {
						return m_queue.take();
					} catch (InterruptedException e) {
						return -1;
					}
				}
			};
		}

		public InputStream getInputStream() {
			return m_responseStream;
		}

		@Override
		public void write(int i) throws IOException {
			String character = new String(new byte[] { (byte) i });
			m_buffer.append(character);
			if (m_buffer.toString().equals(m_expect)) {
				new Timer(true).schedule(new TimerTask() {
					@Override
					public void run() {
						try {
							for (byte b : m_response.getBytes()) {
								m_queue.put(b);
							}
							m_queue.put(new Byte((byte) '\n'));
						} catch (InterruptedException e) {
							System.err
									.println("Something crazy happened (failure)");
						}
					}
				}, m_delayMs);
			}
		}
	}
}
