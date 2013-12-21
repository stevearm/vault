package com.horsefire.couchdb;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * When being run from CouchDB's os_daemon system in local.ini, all output from
 * stdout must be formatted properly
 * 
 * Must not use logging in this class
 */
class CouchDbLogger extends OutputStream {

	private final PrintStream m_delegate;
	private final StringBuilder m_buffer = new StringBuilder();
	private final String m_newLine;

	public CouchDbLogger(PrintStream delegate) {
		m_delegate = delegate;

		m_newLine = System.getProperty("line.separator");
	}

	@Override
	public void write(int b) throws IOException {
		synchronized (m_buffer) {
			m_buffer.append((char) b);
			String buffer = m_buffer.toString();
			if (buffer.endsWith(m_newLine)) {
				String message = buffer.substring(0, buffer.length()
						- m_newLine.length());
				message = message.replace("\\", "\\\\");
				message = message.replace("\"", "\\\"");
				message = message.replace("\n", "\\n");
				m_delegate.print("[\"log\", \"" + message + "\"]\n");
				m_buffer.setLength(0);
			}
		}
	}
}
