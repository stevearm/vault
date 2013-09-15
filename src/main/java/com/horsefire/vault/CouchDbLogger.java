package com.horsefire.vault;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public class CouchDbLogger extends OutputStream {

	public static void install() {
		PrintStream newStdOut = new PrintStream(new CouchDbLogger(System.out));
		System.setOut(newStdOut);
	}

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
				String message = buffer.substring(0, buffer.length() - 2)
						.replace("\"", "\\\"");
				m_delegate.print("[\"log\", \"" + message + "\"]\n");
				m_buffer.setLength(0);
			}
		}
	}
}
