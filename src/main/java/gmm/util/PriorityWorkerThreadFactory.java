package gmm.util;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;
import java.util.concurrent.ForkJoinWorkerThread;

public class PriorityWorkerThreadFactory implements ForkJoinWorkerThreadFactory {
	
	private final int threadPriority;
	
	public PriorityWorkerThreadFactory(int threadPriority) {
		this.threadPriority = threadPriority;
	}

	@Override           
    public ForkJoinWorkerThread newThread(ForkJoinPool pool)
    {
        final ForkJoinWorkerThread worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
        worker.setPriority(threadPriority);
        return worker;
    }
}