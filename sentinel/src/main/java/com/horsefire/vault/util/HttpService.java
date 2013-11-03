package com.horsefire.vault.util;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;

/**
 * This service exists mostly to provide an easy-to-mock http client
 */
public class HttpService {

	private static final Logger LOG = LoggerFactory
			.getLogger(HttpService.class);

	public static class HttpServiceResponse {
		public int responseCode;
		public Header[] headers;
		public byte[] body;
	}

	public HttpServiceResponse head(String url) throws IOException {
		return execute(new HttpHead(url));
	}

	public HttpServiceResponse get(String url) throws IOException {
		return execute(new HttpGet(url));
	}

	private HttpServiceResponse execute(HttpUriRequest method)
			throws IOException {
		DefaultHttpClient client = new DefaultHttpClient();
		InputStream in = null;
		try {
			HttpResponse result = client.execute(method);

			HttpServiceResponse response = new HttpServiceResponse();

			if (result.getEntity() != null) {
				in = result.getEntity().getContent();
				response.body = ByteStreams.toByteArray(in);
			}

			response.responseCode = result.getStatusLine().getStatusCode();
			response.headers = result.getAllHeaders();

			LOG.debug("{} returned {}", method, response.responseCode);
			return response;
		} finally {
			if (in != null) {
				in.close();
			}
			client.getConnectionManager().shutdown();
		}
	}

	public static void main(String[] args) throws IOException {
		HttpServiceResponse response = new HttpService()
				.get("http://127.0.0.1:5984/vault/_design/ui/index.html");
		System.out.println("Response code is " + response.responseCode);
		System.out.println("Body is "
				+ (response.body == null ? "empty"
						: (response.body.length + " bytes")));
		for (Header header : response.headers) {
			System.out.println(header.getName() + ": " + header.getValue());
		}
	}
}
