package com.horsefire.vault;

import com.beust.jcommander.Parameter;

public class CmdArgs {

	@Parameter(names = { "-h", "--help" }, description = "Display help", help = true)
	public boolean help = false;

	@Parameter(names = { "-v", "--version" }, description = "Display version", help = true)
	public boolean version = false;

	@Parameter(names = { "--exportLogConfig" }, description = "Write out a sample log config", help = true)
	public boolean exportLogConfig = false;

	@Parameter(names = { "--logConfigFile" }, description = "Log config file (export to, and read from)")
	public String logConfigFile = "vault.xml";

	@Parameter(names = { "--dbHost" }, description = "Database host")
	public String dbHost = "127.0.0.1";

	@Parameter(names = { "--dbPort" }, description = "Database port")
	public int dbPort = 5984;

	@Parameter(names = { "--dbUsername" }, description = "Username for connecting to vault", required = true)
	public String dbUsername;

	@Parameter(names = { "--dbPassword" }, description = "Password for connecting to vault", required = true)
	public String dbPassword;

	@Parameter(names = { "--dbVaultName" }, description = "Public db name")
	public String dbVault = "vault";

	@Parameter(names = { "--dbVaultDbName" }, description = "Private db name")
	public String dbVaultDb = "vaultdb";

	@Parameter(names = { "--id" }, description = "The id of this vault", required = true)
	public String id;

	@Parameter(names = { "--runtime" }, description = "Quit vault after X seconds")
	public int runtimeSeconds = 60 * 60;

	@Parameter(names = { "--testing" }, hidden = true)
	public boolean testing = false;
}
