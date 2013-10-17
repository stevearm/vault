package com.horsefire.vault.couch;

import java.io.IOException;

import com.google.gson.GsonBuilder;
import com.google.inject.Provider;
import com.horsefire.vault.CouchDbClientFactory;
import com.horsefire.vault.GsonBuilderProvider;
import com.horsefire.vault.SimpleHttpClient;

public class SyncServiceTester {

	public static void main(String[] args) throws IOException {
		String dbHost = "127.0.0.1";
		int dbPort = 5984;
		String dbUsername = "username";
		String dbPassword = "password";
		String vaultId = "vaultUuid";

		Provider<GsonBuilder> gsonBuilderProvider = new GsonBuilderProvider();
		CouchDbClientFactory factory = new CouchDbClientFactory(dbHost, dbPort,
				dbUsername, dbPassword, gsonBuilderProvider);

		new SyncService(factory, vaultId, new SimpleHttpClient()).sync();
	}
}
