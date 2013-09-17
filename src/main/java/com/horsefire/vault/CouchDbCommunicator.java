package com.horsefire.vault;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CouchDbCommunicator {

	private static final Logger LOG = LoggerFactory
			.getLogger(CouchDbCommunicator.class);

	private final PrintStream m_out;
	private final InputStream m_in;

	public CouchDbCommunicator(PrintStream out, InputStream in) {
		m_out = out;
		m_in = in;
	}

	private String get(String category, String key) throws IOException {
		String command = "[\"get\", \"" + category + "\", \"" + key + "\"]\n";
		LOG.debug("Sending command to CouchDB: {}", command);
		m_out.print(command);
		StringBuilder response = new StringBuilder();
		LOG.trace("Awaiting response");
		int read = m_in.read();
		while (read != -1) {
			char character = (char) read;
			if (character == '\n') {
				if (response.length() == 0
						|| "null".equals(response.toString())) {
					LOG.debug("Got no response");
					return null;
				}
				String tmp = response.toString();
				if (tmp.charAt(0) == '"' && tmp.charAt(tmp.length() - 1) == '"') {
					tmp = tmp.substring(1, tmp.length() - 1);
					LOG.debug("Got response: {}", tmp);
					return tmp;
				}
				throw new IOException("Response is missing quotes");
			}
			response.append(character);
			LOG.trace("Response so far: {}", response);
			read = m_in.read();
		}
		throw new IOException("Input stream closed");
	}

	public String getBindAddress() throws IOException {
		return get("httpd", "bind_address");
	}

	public String getPort() throws IOException {
		return get("httpd", "port");
	}

	public String getUuid() throws IOException {
		return get("couchdb", "uuid");
	}
}
