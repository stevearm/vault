package com.horsefire.vault;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.google.common.io.ByteStreams;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.horsefire.vault.couch.CouchDbCommunicator;
import com.horsefire.vault.couch.CouchDbLogger;
import com.horsefire.vault.util.TimeoutInputStream;

public class Bootstrap {

	public static void main(String[] args) throws Exception {
		Options options = getOptions(args);
		if (options == null) {
			return;
		}

		File externalLogConfig = new File(options.logConfigFile);
		if (options.exportLogConfig) {
			exportLogConfig(externalLogConfig);
			return;
		}
		if (externalLogConfig.isFile()) {
			System.setProperty("logback.configurationFile",
					externalLogConfig.getAbsolutePath());
		}

		TimeoutInputStream fixStdIo = null;
		if (options.sentinel) {
			fixStdIo = fixStdIo(options);
		}

		if (missingArguments(options)) {
			return;
		}

		Quitter quitter = new Quitter(options.runtimeSeconds * 1000, fixStdIo);

		Injector injector = Guice.createInjector(new GuiceModule(
				options.dbHost, options.dbPort, options.dbUsername,
				options.dbPassword, options.id, quitter));
		injector.getInstance(Vault.class).run();
	}

	private static Options getOptions(String[] args) {
		try {
			Options options = new Options();
			JCommander jc = new JCommander(options, args);
			jc.setProgramName("java -jar filecabinet.jar");

			if (options.help) {
				jc.usage();
				return null;
			}

			if (options.version) {
				String version = Bootstrap.class.getPackage()
						.getImplementationVersion();
				System.out.println("Vault " + version);
				return null;
			}
			return options;
		} catch (ParameterException e) {
			System.err.println(e.getMessage());
			System.err.println("Use --help to display usage");
			return null;
		}
	}

	private static void exportLogConfig(File externalLogConfig)
			throws IOException {
		InputStream in = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream("logback.xml");
		FileOutputStream out = new FileOutputStream(externalLogConfig);
		try {
			ByteStreams.copy(in, out);
		} finally {
			in.close();
			out.close();
		}
	}

	private static TimeoutInputStream fixStdIo(Options options)
			throws IOException {
		PrintStream externalStdout = System.out;
		CouchDbLogger.install();
		TimeoutInputStream newStdIn = new TimeoutInputStream(System.in, 1000);
		CouchDbCommunicator communicator = new CouchDbCommunicator(
				externalStdout, newStdIn);

		options.dbHost = communicator.getBindAddress();
		options.dbPort = Integer.parseInt(communicator.getPort());
		options.dbUsername = communicator.getUsername();
		options.dbPassword = communicator.getPassword();

		options.id = communicator.getUuid();

		return newStdIn;
	}

	private static boolean missingArguments(Options options) {
		if (options.dbHost == null) {
			System.out.println("Missing dbHost");
			return true;
		}
		if (options.id == null) {
			System.out.println("Missing id");
			return true;
		}
		return false;
	}
}
