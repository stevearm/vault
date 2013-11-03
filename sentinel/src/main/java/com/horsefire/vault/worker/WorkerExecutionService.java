package com.horsefire.vault.worker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.horsefire.vault.util.HttpService;
import com.horsefire.vault.util.HttpService.HttpServiceResponse;

public class WorkerExecutionService {

	private static final Logger LOG = LoggerFactory
			.getLogger(WorkerExecutionService.class);

	static final String CONTENT_MD5 = "Content-MD5";

	private final String m_baseUrl;
	private final HttpService m_httpService;
	private final Map<String, WorkerNode> m_files = new HashMap<String, WorkerNode>();

	private static class WorkerNode {
		public String md5;
		public File file;
	}

	@Inject
	public WorkerExecutionService(@Named("dbHost") String dbHost,
			@Named("dbPort") Integer dbPort,
			@Named("dbUsername") String dbUsername,
			@Named("dbPassword") String dbPassword, HttpService httpService) {
		String auth = "";
		if (dbUsername != null && !dbUsername.isEmpty() && dbPassword != null
				&& !dbPassword.isEmpty()) {
			auth = dbUsername + ":" + dbPassword + "@";
		}
		m_baseUrl = "http://" + auth + dbHost + ":" + dbPort;
		m_httpService = httpService;
	}

	public void runWorker(String db, String ui) throws IOException {
		String url = m_baseUrl + "/" + db + "/_design/" + ui + "/worker.jar";
		String hash = db + "/" + ui;
		File file = null;
		synchronized (m_files) {
			WorkerNode workerNode = m_files.get(hash);
			if (workerNode != null) {
				String currentMd5 = getMd5(url);
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
				workerNode.file = File.createTempFile("worker", "jar");
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

		System.out.println("Should run the jar at " + file);
	}

	private String getMd5(String url) throws IOException {
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
