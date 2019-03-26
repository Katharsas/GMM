package gmm.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class ThreadUtil {

	/**
	 * @return true if thread pool could be terminated.
	 */
	public static boolean shutdownThreadPool(ExecutorService threadPool, int waitInSeconds) {
		threadPool.shutdown();
		try {
			threadPool.awaitTermination(waitInSeconds, TimeUnit.SECONDS);
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
		threadPool.shutdownNow();
		try {
			threadPool.awaitTermination(1, TimeUnit.SECONDS);
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
		return threadPool.isTerminated();
	}
}
