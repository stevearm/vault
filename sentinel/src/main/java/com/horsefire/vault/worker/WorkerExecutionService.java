package com.horsefire.vault.worker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.horsefire.vault.util.CommandExecutionService;
import com.horsefire.vault.util.HttpService;
import com.horsefire.vault.util.HttpService.HttpServiceResponse;

public class WorkerExecutionService {

	private static final Logger LOG = LoggerFactory
			.getLogger(WorkerExecutionService.class);

	static final String CONTENT_MD5 = "Content-MD5";

	private final String m_baseUrl;
	private final String[] m_argsList;
	private final HttpService m_httpService;
	private final CommandExecutionService m_cmdExecService;
	private final Map<String, WorkerNode> m_files = new HashMap<String, WorkerNode>();

	private static class WorkerNode {
		public String md5;
		public File file;
	}

	@Inject
	public WorkerExecutionService(@Named("dbHost") String dbHost,
			@Named("dbPort") Integer dbPort,
			@Named("dbUsername") String dbUsername,
			@Named("dbPassword") String dbPassword, HttpService httpService,
			CommandExecutionService cmdExecService) {
		String httpAuth = "";
		String[] cmdAuth = new String[0];
		if (dbUsername != null && !dbUsername.isEmpty() && dbPassword != null
				&& !dbPassword.isEmpty()) {
			httpAuth = dbUsername + ":" + dbPassword + "@";
			cmdAuth = new String[] { " --username", dbUsername, "--password",
					dbPassword };
		}
		String[] args = new String[] { "--host", dbHost, "--port", "" + dbPort };
		if (cmdAuth.length > 0) {
			args = Arrays.copyOf(args, args.length + cmdAuth.length);
			System.arraycopy(cmdAuth, 0, args, args.length - cmdAuth.length,
					cmdAuth.length);
		}
		m_argsList = args;
		m_baseUrl = "http://" + httpAuth + dbHost + ":" + dbPort;
		m_httpService = httpService;
		m_cmdExecService = cmdExecService;
	}

	public void runWorker(String db, String ui) throws IOException,
			InterruptedException {
		final String url = m_baseUrl + "/" + db + "/_design/" + ui
				+ "/worker.jar";
		final String hash = db + "/" + ui;
		File file = null;
		synchronized (m_files) {
			WorkerNode workerNode = m_files.get(hash);
			if (workerNode != null) {
				String currentMd5 = checkMd5(url);
				if (!workerNode.md5.equals(currentMd5)) {
					workerNode = null;
				}
			}
			if (workerNode == null) {
				HttpServiceResponse jarResponse = m_httpService.get(url);
				if (jarResponse.responseCode != HttpURLConnection.HTTP_OK) {
					String responseText = new String(jarResponse.body, "UTF-8");
					LOG.info("GET {} returned {}: {}", new Object[] { url,
							jarResponse.responseCode, responseText });
					throw new IOException("GET " + url + " returned "
							+ jarResponse.responseCode + ": " + responseText);
				}

				workerNode = new WorkerNode();
				workerNode.md5 = getMd5(jarResponse.headers);
				workerNode.file = File.createTempFile("worker", ".jar");
				workerNode.file.deleteOnExit();
				FileOutputStream out = new FileOutputStream(workerNode.file);
				try {
					out.write(jarResponse.body);
				} finally {
					out.close();
				}

				m_files.put(hash, workerNode);
			}
			file = workerNode.file;
		}

		String[] command = new String[] { "java", "-jar",
				file.getAbsolutePath(), "--db", db };
		command = Arrays.copyOf(command, command.length + m_argsList.length);
		System.arraycopy(m_argsList, 0, command, command.length
				- m_argsList.length, m_argsList.length);
		m_cmdExecService.run(command);
	}

	private String checkMd5(String url) throws IOException {
		HttpServiceResponse head = m_httpService.head(url);
		if (head.responseCode == HttpURLConnection.HTTP_OK) {
			String md5 = getMd5(head.headers);
			if (md5 == null) {
				LOG.warn("HEAD {} has no '{}' header", url, CONTENT_MD5);
				throw new IOException("Missing '" + CONTENT_MD5 + "' header");
			}
			return md5;
		}
		throw new IOException("Error response: " + head.responseCode);
	}

	private String getMd5(Header[] response) {
		for (Header header : response) {
			if (header.getName().equals(CONTENT_MD5)) {
				return header.getValue();
			}
		}
		return null;
	}
}
