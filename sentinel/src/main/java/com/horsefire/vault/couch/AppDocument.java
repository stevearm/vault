package com.horsefire.vault.couch;

public class AppDocument {

	public static final String TYPE = "app";

	public String _id;
	public String _rev;
	@SuppressWarnings("unused")
	private String type;

	public String name;
	public String database;
	public String ui;
	public String worker;
}
