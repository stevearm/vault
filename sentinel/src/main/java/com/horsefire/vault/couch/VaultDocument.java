package com.horsefire.vault.couch;

import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

public class VaultDocument {

	public static final String TYPE = "vault";

	public String _id;
	public String _rev;
	@SuppressWarnings("unused")
	private String type = TYPE;

	public String name;
	public int priority;
	public JsonObject signature;

	public String username;
	public String password;

	// Connection info (only if externally accessible)
	public String host;
	public int port;

	public List<String> dbs;

	public enum Direction {
		@SerializedName("push")
		PUSH,

		@SerializedName("pull")
		PULL,

		@SerializedName("both")
		BOTH
	}
}
