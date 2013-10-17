package com.horsefire.vault;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.CharStreams;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class SimpleHttpClient {

	private static final Logger LOG = LoggerFactory
			.getLogger(SimpleHttpClient.class);

	public JsonObject get(String host, Integer port, String fragment)
			throws IOException {
		DefaultHttpClient client = new DefaultHttpClient();
		Reader in = null;
		try {
			String url = "http://" + host + ":" + port + fragment;
			HttpResponse result = client.execute(new HttpGet(url));
			in = new InputStreamReader(result.getEntity().getContent(), "UTF-8");
			int responseCode = result.getStatusLine().getStatusCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
				JsonObject response = new JsonParser().parse(in)
						.getAsJsonObject();
				LOG.debug("GET {} returned {}: {}", new Object[] { url,
						responseCode, response });
				return response;
			}
			String response = CharStreams.toString(in);
			LOG.info("GET {} returned {}: {}", new Object[] { url,
					responseCode, response });
			throw new IOException("GET " + url + " returned " + responseCode
					+ ": " + response);
		} finally {
			if (in != null) {
				in.close();
			}
			client.getConnectionManager().shutdown();
		}
	}
}
