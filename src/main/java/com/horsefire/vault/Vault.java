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
import com.google.inject.Inject;
import com.horsefire.vault.util.TimeoutInputStream;

public class Vault {

	private final Options m_options;
	private final Sentinel.Factory m_sentinelFactory;

	@Inject
	public Vault(Options options, Sentinel.Factory sentinelFactory) {
		m_options = options;
		m_sentinelFactory = sentinelFactory;
	}

	private void exportLogConfig(File externalLogConfig) throws IOException {
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

	public void run() throws IOException {
		File externalLogConfig = new File(m_options.logConfigFile);

		if (m_options.exportLogConfig) {
			exportLogConfig(externalLogConfig);
			return;
		}

		if (externalLogConfig.isFile()) {
			System.setProperty("logback.configurationFile",
					externalLogConfig.getAbsolutePath());
		}

		String host = m_options.dbHost;
		String id = m_options.id;
		long quittingTime = System.currentTimeMillis()
				+ (m_options.runtimeSeconds * 1000);
		Quitter quitter;

		if (!m_options.sentinel) {
			if (host == null) {
				System.out.println("Must specify a host");
				return;
			}
			if (id == null) {
				System.out.println("Must specify an id");
				return;
			}

			quitter = new Quitter(quittingTime);
		} else {
			PrintStream externalStdout = System.out;
			CouchDbLogger.install();
			TimeoutInputStream newStdIn = new TimeoutInputStream(System.in,
					1000);
			CouchDbCommunicator communicator = new CouchDbCommunicator(
					externalStdout, newStdIn);

			String ip = communicator.getBindAddress();
			String port = communicator.getPort();
			if (ip == null || port == null) {
				System.out.println("Error getting ip and port");
				throw new IOException("Error getting ip and port");
			}
			host = ip + ":" + port;

			id = communicator.getUuid();
			if (id == null) {
				System.out.println("Error getting uuid");
				throw new IOException("Error getting uuid");
			}

			quitter = new Quitter(quittingTime, newStdIn);
		}

		m_sentinelFactory.create(host, id, quitter).run();
	}

	public static void main(String[] args) throws Exception {
		Options options = new Options();
		try {
			JCommander jc = new JCommander(options, args);
			jc.setProgramName("java -jar filecabinet.jar");

			if (options.help) {
				jc.usage();
				return;
			}

			if (options.version) {
				String version = Vault.class.getPackage()
						.getImplementationVersion();
				System.out.println("Vault " + version);
				return;
			}
		} catch (ParameterException e) {
			System.err.println(e.getMessage());
			System.err.println("Use --help to display usage");
			return;
		}

		Guice.createInjector(new VaultModule(options)).getInstance(Vault.class)
				.run();
	}
}
