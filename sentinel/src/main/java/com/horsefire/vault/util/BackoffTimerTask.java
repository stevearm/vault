package com.horsefire.vault.util;

public interface BackoffTimerTask {

	/**
	 * If the task returns true, it will be rescheduled normally. If it returns
	 * false, it'll be run again 1 second later. Multiple errors will continue
	 * to retry with an increasing backoff.
	 * 
	 * @return true if the task ran fine
	 */
	boolean run();
}
