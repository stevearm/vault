package com.horsefire.vault.couch;

import junit.framework.TestCase;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import com.google.gson.Gson;
import com.horsefire.vault.GsonBuilderProvider;

public class WorkerDocumentTest extends TestCase {

	@Test
	public void testDeserialization() {
		Gson gson = new GsonBuilderProvider().get().create();

		WorkerDocument doc = gson.fromJson(
				"{\"triggered\":\"2013-03-24T18:45:20.01Z\"}",
				WorkerDocument.class);
		assertTrue(new DateTime(2013, 3, 24, 18, 45, 20, 10, DateTimeZone.UTC)
				.compareTo(doc.triggered) == 0);
	}

	@Test
	public void testSerialization() {
		Gson gson = new GsonBuilderProvider().get().create();

		WorkerDocument doc = new WorkerDocument();
		doc.triggered = new DateTime(2013, 3, 24, 18, 45, 20, 10,
				DateTimeZone.UTC);

		String json = gson.toJson(doc);
		assertEquals("{\"triggered\":\"2013-03-24T18:45:20.010Z\"}", json);
	}
}
