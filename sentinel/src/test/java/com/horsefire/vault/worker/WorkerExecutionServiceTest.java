package com.horsefire.vault.worker;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import junit.framework.TestCase;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.ParseException;
import org.junit.Test;

import com.horsefire.vault.util.HttpService;
import com.horsefire.vault.util.HttpService.HttpServiceResponse;

public class WorkerExecutionServiceTest extends TestCase {

	@Test
	public void testWorking() throws Exception {
		HttpService httpServiceMock = createMock(HttpService.class);

		HttpServiceResponse getResponse = new HttpServiceResponse();
		getResponse.responseCode = 200;
		getResponse.headers = new Header[] {
				new TestHeader("Content-Length", "3"),
				new TestHeader(WorkerExecutionService.CONTENT_MD5, "sjofijoei") };
		getResponse.body = new byte[] { 2, 3, 4 };
		expect(
				httpServiceMock
						.get(eq("http://myhost:80/mydb/_design/myui/worker.jar")))
				.andReturn(getResponse).once();

		HttpServiceResponse headResponse = new HttpServiceResponse();
		headResponse.responseCode = 200;
		headResponse.headers = getResponse.headers;
		expect(
				httpServiceMock
						.head(eq("http://myhost:80/mydb/_design/myui/worker.jar")))
				.andReturn(headResponse).once();

		headResponse = new HttpServiceResponse();
		headResponse.responseCode = 200;
		headResponse.headers = new Header[] {
				new TestHeader("Content-Length", "3"),
				new TestHeader(WorkerExecutionService.CONTENT_MD5, "sjofrerere") };
		expect(
				httpServiceMock
						.head(eq("http://myhost:80/mydb/_design/myui/worker.jar")))
				.andReturn(headResponse).once();

		getResponse = new HttpServiceResponse();
		getResponse.responseCode = 200;
		getResponse.headers = headResponse.headers;
		getResponse.body = new byte[] { 2, 3, 4 };
		expect(
				httpServiceMock
						.get(eq("http://myhost:80/mydb/_design/myui/worker.jar")))
				.andReturn(getResponse).once();

		WorkerExecutionService service = new WorkerExecutionService("myhost",
				80, null, null, httpServiceMock);

		replay(httpServiceMock);
		service.runWorker("mydb", "myui");
		service.runWorker("mydb", "myui");
		service.runWorker("mydb", "myui");
		verify(httpServiceMock);
	}

	private static class TestHeader implements Header {

		private final String m_name;
		private final String m_value;

		public TestHeader(String name, String value) {
			m_name = name;
			m_value = value;
		}

		public String getName() {
			return m_name;
		}

		public String getValue() {
			return m_value;
		}

		public HeaderElement[] getElements() throws ParseException {
			throw new UnsupportedOperationException();
		}
	}
}
