package com.horsefire.vault.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandExecutionService {

	private static final Logger LOG = LoggerFactory
			.getLogger(CommandExecutionService.class);

	/**
	 * A blocking call that runs the given command
	 * 
	 * @param command
	 *            the command to run
	 * @return the exit code
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public int run(String[] command) throws IOException, InterruptedException {
		Process proc = Runtime.getRuntime().exec(command);
		int code = System.identityHashCode(proc);
		LOG.info("Running command as {} id: {}", code, command);
		new Thread(new StreamLogger(code, proc.getInputStream(), "stdout"))
				.start();
		new Thread(new StreamLogger(code, proc.getErrorStream(), "stderr"))
				.start();
		return proc.waitFor();
	}

	private static class StreamLogger implements Runnable {

		private String m_loggerName;
		private InputStream m_in;

		public StreamLogger(int id, InputStream in, String type) {
			m_loggerName = CommandExecutionService.class.getCanonicalName()
					+ "." + id + "." + type;
			m_in = in;
		}

		public void run() {
			final Logger logger = LoggerFactory.getLogger(m_loggerName);
			logger.info("Starting to drain output buffer");

			BufferedReader reader = new BufferedReader(new InputStreamReader(
					m_in));
			while (true) {
				try {
					String line = reader.readLine();
					if (line == null) {
						return;
					}
					logger.info(line);
				} catch (IOException e) {
					logger.warn("Problem draining output buffer", e);
				}
			}
		}
	}
}
