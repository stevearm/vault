package com.horsefire.vault;

import java.io.IOException;
import java.net.HttpURLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.horsefire.vault.util.HttpService;
import com.horsefire.vault.util.HttpService.HttpServiceResponse;

public class SimpleHttpClient {

	private static final Logger LOG = LoggerFactory
			.getLogger(SimpleHttpClient.class);

	private final HttpService m_service;

	@Inject
	public SimpleHttpClient(HttpService service) {
		m_service = service;
	}

	public JsonObject get(String host, Integer port, String fragment)
			throws IOException {
		return get(host, port, null, null, fragment);
	}

	public JsonObject get(String host, Integer port, String username,
			String password, String fragment) throws IOException {
		String auth = "";
		if (username != null && !username.isEmpty() && password != null
				&& !password.isEmpty()) {
			auth = username + ":" + password + "@";
		}
		String url = "http://" + auth + host + ":" + port + fragment;
		HttpServiceResponse response = m_service.get(url);
		String responseText = new String(response.body, "UTF-8");
		if (response.responseCode == HttpURLConnection.HTTP_OK) {
			JsonObject responseObject = new JsonParser().parse(responseText)
					.getAsJsonObject();
			LOG.debug("GET {} returned {}: {}", new Object[] { url,
					response.responseCode, responseObject });
			return responseObject;
		}
		LOG.info("GET {} returned {}: {}", new Object[] { url,
				response.responseCode, responseText });
		throw new IOException("GET " + url + " returned "
				+ response.responseCode + ": " + responseText);
	}
}
