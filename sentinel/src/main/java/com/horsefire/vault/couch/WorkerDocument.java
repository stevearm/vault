package com.horsefire.vault.couch;

import org.joda.time.DateTime;

public class WorkerDocument {

	public String _id;
	public String _rev;

	public DateTime triggered;
	public DateTime started;
	public DateTime finished;
}
