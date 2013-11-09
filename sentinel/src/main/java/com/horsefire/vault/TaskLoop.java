package com.horsefire.vault;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class TaskLoop {

	public static interface Task {
		long periodMs();

		void run();
	}

	private class TaskNode {
		Task task;
		long nextRun;
	}

	private static final Logger LOG = LoggerFactory.getLogger(TaskLoop.class);

	private static final long SLEEP_TIME_MS = 1000;
	private static final long GRACE_PERIOD_MS = 5 * 60 * 1000;

	private final Quitter m_quitter;
	private final List<TaskNode> m_tasks = new ArrayList<TaskNode>();
	private boolean m_started = false;

	@Inject
	public TaskLoop(Quitter quitter) {
		m_quitter = quitter;
	}

	public void addTask(Task task) {
		TaskNode node = new TaskNode();
		node.task = task;
		node.nextRun = 0;
		synchronized (m_tasks) {
			if (m_started) {
				throw new IllegalStateException(
						"Cannot add new tasks after starting loop");
			}
			m_tasks.add(node);
		}
	}

	/**
	 * Spin up a new thread and run the task loop. The thread should gracefully
	 * stop when the Quitter says so. If the VM is still running
	 * {@link GRACE_PERIOD_MS} milliseconds after the {@link Quitter} says to
	 * quit, this thread will do a {@link System#exit(1)}
	 */
	public void run() {
		synchronized (m_tasks) {
			if (m_started) {
				throw new IllegalStateException("Can only start loop once");
			}
			m_started = true;

			startSentinel();
			startLoop();
		}
	}

	private void startSentinel() {
		final Quitter quitter = m_quitter;
		Timer timer = new Timer("TaskLoop-sentinel", true);
		timer.schedule(new TimerTask() {

			private long m_overdueTime = 0;

			@Override
			public void run() {
				if (quitter.shouldQuit()) {
					if (m_overdueTime == 0) {
						m_overdueTime = System.currentTimeMillis()
								+ GRACE_PERIOD_MS;
					}
					if (m_overdueTime < System.currentTimeMillis()) {
						System.exit(1);
					}
				}
			}
		}, 1000, 1000);
	}

	private void startLoop() {
		new Thread("TaskLooper") {
			@Override
			public void run() {
				loop();
			}
		}.start();
	}

	void loop() {
		LOG.info("Starting TaskLoop");
		while (!m_quitter.shouldQuit()) {
			long now = System.currentTimeMillis();
			for (TaskNode task : m_tasks) {
				if (task.nextRun < now) {
					task.task.run();
					task.nextRun = now + task.task.periodMs();
				}
			}

			try {
				Thread.sleep(SLEEP_TIME_MS);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		LOG.info("Finished TaskLoop");
	}
}
