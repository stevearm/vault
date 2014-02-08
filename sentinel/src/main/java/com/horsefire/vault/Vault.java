package com.horsefire.vault;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.google.common.io.ByteStreams;
import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * This is the default main class for the jar, and allows the jar to be run from
 * the command-line
 * 
 * Do not use logging in this class as the logging system needs to be
 * initialized
 */
public class Vault {

	public static final String VERSION;

	static {
		String version = Vault.class.getPackage().getImplementationVersion();
		if (version == null) {
			version = "UNKNOWN-VERSION";
		}
		VERSION = version;
	}

	public static void main(String[] args) throws Exception {
		CmdArgs options = getOptions(args);
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

		final long quittingTime = System.currentTimeMillis()
				+ (options.runtimeSeconds * 1000);
		Quitter quitter = new Quitter() {
			public boolean shouldQuit() {
				return quittingTime < System.currentTimeMillis();
			}
		};

		Injector injector = Guice.createInjector(new GuiceModule(options,
				quitter));
		injector.getInstance(Sentinel.class).run();
	}

	private static CmdArgs getOptions(String[] args) {
		try {
			CmdArgs options = new CmdArgs();
			JCommander jc = new JCommander(options, args);
			jc.setProgramName("java -jar filecabinet.jar");

			if (options.help) {
				jc.usage();
				return null;
			}

			if (options.version) {
				System.out.println("Vault " + VERSION);
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
}
