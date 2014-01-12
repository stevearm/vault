package com.horsefire.vault.couch;

import java.util.ArrayList;
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

	public JsonObject signature;

	public String username;
	public String password;

	// If this vault has a sentinel running, this is the version
	// (if not, this is null)
	public String sentinel;

	// Null if vault is not externally addressable
	public Addressable addressable;

	public List<String> dbs = new ArrayList<String>();

	public enum Direction {
		@SerializedName("push")
		PUSH,

		@SerializedName("pull")
		PULL,

		@SerializedName("both")
		BOTH
	}

	public static class Addressable {
		public String host;
		public int port;
		public int priority; // Highest priority is synced first
		public boolean enabled;
	}
}
