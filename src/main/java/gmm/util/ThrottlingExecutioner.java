package gmm.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for throttling execution of small tasks down to a specified number of executions
 * per time. Useful for throttling sending of simple events, for example.
 * 
 * Uses a single threaded scheduler to execute tasks only after configured delay.
 * 
 * @author Jan Mothes
 */
public class ThrottlingExecutioner {

	private final long delayTimeMillis;
	private final Map<String, ScheduledFuture<?>> delayedTasks;
	private final ScheduledExecutorService executor;
	
	/**
	 * @see {@link #curbYourEnthusiasm(String, Runnable)}
	 */
	public ThrottlingExecutioner(long delayTimeMillis) {
		this.delayTimeMillis = delayTimeMillis;
		delayedTasks = new HashMap<>();
		executor = Executors.newSingleThreadScheduledExecutor();
	}
	
	/**
	 * Thread-safe.
	 * <br><br>
	 * Does not execute a task more than once every {@link #delayTimeMillis} milliseconds.
	 * If that amount of time has already passed for the given task since it was last executed,
	 * the task will be executed immediately. Otherwise, it will be executed when the delay is
	 * over.
	 * <br><br>
	 * Execution of tasks is asynchronous. Tasks must return fast, otherwise execution of any
	 * waiting task may be delayed more than expected. If this method is called faster than
	 * delay allows execution, calls will have no effect (excess tasks won't run). 
	 */
	public void curbYourEnthusiasm(String taskId, Runnable task) {
		synchronized(delayedTasks) {
			final ScheduledFuture<?> waitingTask = delayedTasks.get(taskId);
			final boolean notExistsWaitingTask = waitingTask == null || waitingTask.isDone();
			
			if (notExistsWaitingTask) {
				final ScheduledFuture<?> dummyTaskFuture = 
						executor.schedule(() -> {}, delayTimeMillis, TimeUnit.MILLISECONDS);
				delayedTasks.put(taskId, dummyTaskFuture);
				executor.schedule(task, 0, TimeUnit.MILLISECONDS);
			} else {
				final long remainingDelayMillis = Math.max(waitingTask.getDelay(TimeUnit.MILLISECONDS), 0);
				waitingTask.cancel(false);
				final ScheduledFuture<?> taskFuture = 
						executor.schedule(task, remainingDelayMillis, TimeUnit.MILLISECONDS);
				delayedTasks.put(taskId, taskFuture);
			}
		}
	}
}
