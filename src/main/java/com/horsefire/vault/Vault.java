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

public class Vault {

	private static final File EXTERNAL_LOG_CONFIG = new File("vault.xml");

	private final Options m_options;
	private final Sentinel.Factory m_sentinelFactory;

	public Vault(Options options, Sentinel.Factory sentinelFactory) {
		m_options = options;
		m_sentinelFactory = sentinelFactory;
	}

	private void exportLogConfig() throws IOException {
		InputStream in = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream("logback.xml");
		FileOutputStream out = new FileOutputStream(EXTERNAL_LOG_CONFIG);
		try {
			ByteStreams.copy(in, out);
		} finally {
			in.close();
			out.close();
		}
	}

	public void run() throws IOException {
		if (m_options.exportLogConfig) {
			exportLogConfig();
			return;
		}

		if (EXTERNAL_LOG_CONFIG.isFile()) {
			System.setProperty("logback.configurationFile",
					EXTERNAL_LOG_CONFIG.getAbsolutePath());
		}

		String host = m_options.dbHost;
		String id = m_options.id;

		if (m_options.sentinel) {
			// Install stdout filter, then get new host/id
			PrintStream externalStdout = System.out;
		}

		m_sentinelFactory.create(host, id).run();
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
