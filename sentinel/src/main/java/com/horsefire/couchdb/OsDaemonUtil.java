package com.horsefire.couchdb;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

/**
 * When being run from CouchDB's os_daemon system in local.ini, stdin/stdout
 * must be handled in a specific way
 */
public final class OsDaemonUtil {

	private static final TimeoutInputStream STDIN_WRAPPER;
	private static final CouchDbCommunicator COMMUNICATOR;

	static {
		PrintStream stdOut = System.out;
		InputStream stdIn = System.in;

		System.setOut(new PrintStream(new CouchDbLogger(stdOut)));

		STDIN_WRAPPER = new TimeoutInputStream(stdIn, 1000);
		COMMUNICATOR = new CouchDbCommunicator(stdOut, STDIN_WRAPPER);
	}
	
	private OsDaemonUtil(){}

	public static boolean isStdInOpen() {
		return STDIN_WRAPPER.isStreamOpen();
	}

	public static String getProperty(String category, String key)
			throws IOException {
		return COMMUNICATOR.get(category, key);
	}

	public static String getBindAddress() throws IOException {
		return getProperty("httpd", "bind_address");
	}

	public static int getPort() throws IOException {
		return Integer.parseInt(getProperty("httpd", "port"));
	}

	public static String getUuid() throws IOException {
		return getProperty("couchdb", "uuid");
	}
}
