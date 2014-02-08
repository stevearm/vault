package com.horsefire.vault.couch;

import java.io.IOException;

import com.google.gson.GsonBuilder;
import com.google.inject.Provider;
import com.horsefire.vault.CmdArgs;
import com.horsefire.vault.CouchDbClientFactory;
import com.horsefire.vault.GsonBuilderProvider;
import com.horsefire.vault.SimpleHttpClient;
import com.horsefire.vault.util.HttpService;

public class SyncServiceTester {

	public static void main(String[] args) throws IOException {
		CmdArgs cmdArgs = new CmdArgs();
		cmdArgs.dbHost = "127.0.0.1";
		cmdArgs.dbPort = 5984;
		cmdArgs.dbUsername = "username";
		cmdArgs.dbPassword = "password";
		cmdArgs.id = "vaultUuid";

		Provider<GsonBuilder> gsonBuilderProvider = new GsonBuilderProvider();
		CouchDbClientFactory factory = new CouchDbClientFactory(cmdArgs,
				gsonBuilderProvider);

		new SyncService(factory, cmdArgs, new SimpleHttpClient(
				new HttpService())).run();
	}
}
