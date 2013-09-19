package com.horsefire.vault;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.TestCase;

import org.junit.Test;

import com.horsefire.vault.util.TimeoutInputStream;

public class TimeoutInputStreamTest extends TestCase {

	@Test
	public void testSuccess() throws Exception {
		byte[] expected = new byte[] { 4, 5, 7, 1, 3, 13 };
		TimeoutInputStream test = new TimeoutInputStream(
				new ByteArrayInputStream(expected), 50);
		try {
			byte[] actual = new byte[expected.length];
			assertEquals(actual.length, test.read(actual));
			assertEquals(expected.length, actual.length);
			for (int i = 0; i < expected.length; i++) {
				assertEquals("For entry " + i, expected[i], actual[i]);
			}
		} finally {
			test.close();
		}
	}

	@Test
	public void testTimeout() throws Exception {
		final AtomicBoolean innerReadCompleted = new AtomicBoolean(false);
		final int timeout = 50;
		InputStream fakeInputStream = new InputStream() {
			@Override
			public int read() throws IOException {
				try {
					Thread.sleep(timeout * 10);
				} catch (InterruptedException e) {
				}
				innerReadCompleted.set(true);
				return 1;
			}
		};

		@SuppressWarnings("resource")
		final TimeoutInputStream test = new TimeoutInputStream(fakeInputStream,
				timeout);

		final AtomicBoolean readTimedOut = new AtomicBoolean(false);
		Thread testThread = new Thread("TimeoutInputStreamTest-test-thread") {
			@Override
			public void run() {
				try {
					test.read();
				} catch (IOException e) {
					readTimedOut.set(true);
				}
			}
		};
		testThread.start();

		Thread.sleep(timeout * 2);
		assertTrue("Read should have timed out", readTimedOut.get());
		assertFalse("Fake input stream shouldn't have finished",
				innerReadCompleted.get());
		testThread.interrupt();
	}
}
