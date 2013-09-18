package com.horsefire.vault;

import com.beust.jcommander.Parameter;

public class Options {

	@Parameter(names = { "-h", "--help" }, description = "Display help", help = true)
	public boolean help = false;

	@Parameter(names = { "-v", "--version" }, description = "Display version", help = true)
	public boolean version = false;

	@Parameter(names = { "--sentinel" }, description = "Used when run directly by CouchDB")
	public boolean sentinel = false;

	@Parameter(names = { "--exportLogConfig" }, description = "Write out a sample log config")
	public boolean exportLogConfig = false;

	@Parameter(names = { "--logConfigFile" }, description = "Log config file (export to, and read from)")
	public String logConfigFile = "vault.xml";

	@Parameter(names = { "--dbHost" }, description = "Database host")
	public String dbHost = "127.0.0.1";

	@Parameter(names = { "--dbPort" }, description = "Database port")
	public int dbPort = 5984;

	@Parameter(names = { "--id" }, description = "The id of this vault")
	public String id;

	@Parameter(names = { "--runtime" }, description = "Quit vault after X seconds")
	public int runtimeSeconds = 30 * 60;

	@Parameter(names = { "--debug" }, description = "Use when developing")
	public boolean debug = false;
}
