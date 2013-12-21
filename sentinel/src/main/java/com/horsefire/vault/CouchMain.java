package com.horsefire.vault;

import java.io.File;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.horsefire.couchdb.OsDaemonUtil;

/**
 * This class is meant to be run only by CouchDB (through the os_daemons part of
 * local.ini). It uses stdio to communicate to CouchDB, and so needs to setup
 * some wrappers before doing any output.
 * 
 * Do not use logging in this class because stdout needs to be fixed first, and
 * the log config file needs to be initialized first
 */
public class CouchMain {

	private static final String LOG_CONFIG_FILE = "vault.xml";

	public static void main(String[] args) throws Exception {
		if (args.length == 1) {
			File logConfigFile = new File(args[0], LOG_CONFIG_FILE);
			if (logConfigFile.isFile()) {
				System.setProperty("logback.configurationFile",
						logConfigFile.getAbsolutePath());
			}
		}

		String dbHost = OsDaemonUtil.getBindAddress();
		int dbPort = OsDaemonUtil.getPort();
		String id = OsDaemonUtil.getUuid();
		String dbUsername = OsDaemonUtil.getProperty("vault", "username");
		String dbPassword = OsDaemonUtil.getProperty("vault", "password");
		if (empty(id) || empty(dbUsername) || empty(dbPassword)) {
			System.out.println("Missing uuid, username or password");
			System.exit(1);
		}

		Quitter quitter = new Quitter() {
			public boolean shouldQuit() {
				return !OsDaemonUtil.isStdInOpen();
			}
		};

		Injector injector = Guice.createInjector(new GuiceModule(dbHost,
				dbPort, dbUsername, dbPassword, id, quitter));
		injector.getInstance(Sentinel.class).run();
	}

	private static boolean empty(String string) {
		return string == null || string.isEmpty();
	}
}
