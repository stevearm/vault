package com.horsefire.vault.util;

import java.io.IOException;

import junit.framework.TestCase;

import org.junit.Test;

public class CommandExecutionServiceTest extends TestCase {

	@Test
	public void testJavaIsInPath() throws IOException, InterruptedException {
		CommandExecutionService service = new CommandExecutionService();
		int exitCode = service.run(new String[] { "java", "-version" });
		assertEquals("Java should have exited normally", 0, exitCode);
	}

	@Test
	public void testExitCodeWorks() throws IOException, InterruptedException {
		CommandExecutionService service = new CommandExecutionService();
		int exitCode = service.run(new String[] { "java", "--fffff" });
		assertTrue("Java should have exited with a non-zero error code",
				0 != exitCode);
	}

	@Test
	public void testMissingCommandFails() throws IOException,
			InterruptedException {
		CommandExecutionService service = new CommandExecutionService();
		try {
			service.run(new String[] { "broken" });
			fail("Command doesn't exist. Should have thrown exception");
		} catch (IOException e) {
			// Expected
		}
	}
}
