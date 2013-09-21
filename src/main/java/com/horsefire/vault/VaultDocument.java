package com.horsefire.vault;

import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

public class VaultDocument {

	public String _id;
	public String _rev;
	public String name;
	public JsonObject signature;
	public List<SyncTarget> sync;

	public static class SyncTarget {
		public String id;
		public List<DbTarget> dbs;
	}

	public static class DbTarget {
		public String local;
		public String remote;
		public Direction direction;
	}

	public enum Direction {
		@SerializedName("push")
		PUSH,

		@SerializedName("pull")
		PULL,

		@SerializedName("both")
		BOTH
	}
}