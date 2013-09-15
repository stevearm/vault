package com.horsefire.vault;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.horsefire.vault.util.TimeoutInputStream;

public class CouchDbCommunicator {

	private static final Logger LOG = LoggerFactory
			.getLogger(CouchDbCommunicator.class);

	private final PrintStream m_out;
	private final InputStream m_in;

	public CouchDbCommunicator(PrintStream out, InputStream in) {
		m_out = out;
		m_in = new TimeoutInputStream(in, 1000);
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
				String tmp = response.toString();
				LOG.debug("Got response: {}", tmp);
				return tmp;
			}
			response.append(character);
			LOG.trace("Response so far: {}", response);
			read = m_in.read();
		}
		throw new IOException("Input stream closed");
	}

	public String getHost() throws IOException {
		return get("httpd", "port") + ":" + get("httpd", "bind_address");
	}

	public String getId() throws IOException {
		return get("couchdb", "uuid");
	}
}
