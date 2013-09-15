package com.horsefire.vault;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import junit.framework.TestCase;

import org.junit.Test;

public class CouchDbLoggerTest extends TestCase {

	private PrintStream m_testStream;
	private ByteArrayOutputStream m_resultStream;

	@Override
	protected void setUp() throws Exception {
		m_resultStream = new ByteArrayOutputStream();
		m_testStream = new PrintStream(new CouchDbLogger(new PrintStream(
				m_resultStream)));
	}

	private void assertTranslated(String source, String result) {
		m_testStream.println(source);
		assertEquals(result + "\n", new String(m_resultStream.toByteArray()));
		m_resultStream.reset();
	}

	@Test
	public void testStandard() {
		assertTranslated("Testing", "[\"log\", \"Testing\"]");
	}

	@Test
	public void testQuotes() {
		assertTranslated("Test\"Quotes", "[\"log\", \"Test\\\"Quotes\"]");
	}

	@Test
	public void testNoNewLine() {
		m_testStream.print("First line");
		assertTranslated("Second line", "[\"log\", \"First lineSecond line\"]");
	}
}
