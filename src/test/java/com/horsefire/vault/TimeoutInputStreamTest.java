package com.horsefire.vault;

import java.io.ByteArrayInputStream;

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
}
