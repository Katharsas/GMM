package gmm.service.assets;

import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gmm.web.WebSocketEventSender;
import gmm.web.WebSocketEventSender.WebSocketEvent;

/**
 * Manages ownership of code over new asset folder. Combines two locking mechanisms:
 * <br>
 * <br> - Lock: Used to synchronize normal (non-async), short-lived threads
 * <br> - Flag: Used by those threads before unlocking to "extend" the "lock" beyond their own lifetime
 * 			when they activate worker threads to do preview processing, so that other threads cannot 
 * 			acquire the lock until workers are done with async tasks.
 * 
 * @author Jan Mothes
 */
@Service
public class NewAssetLockService {
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private final WebSocketEventSender eventSender;
	
	private final ReentrantLock newAssetOperationsLock = new ReentrantLock(true); // lock
	private volatile boolean isClosed = false; // flag
	
	@Autowired
	public NewAssetLockService(WebSocketEventSender eventSender) {
		this.eventSender = eventSender;
//		testBlock();
	}
	
	/**
	 * Will block calling thread until lock is both open and has been acquired.
	 * @see {@link ReentrantLock#lock()}
	 */
	public synchronized void lock(String actor) {
		while (isClosed || !newAssetOperationsLock.tryLock()) {
			try {
				wait();
			} catch (final InterruptedException e) {}
		}
		logger.info("Lock acquired. (by '" + actor + "')");
		eventSender.broadcastEvent(WebSocketEvent.AssetFileOperationsChangeEvent);
	}
	
	/**
	 * @return True if lock is both open and calling thread could acquire lock.
	 * @see {@link ReentrantLock#tryLock()}
	 */
	public synchronized boolean tryLock(String actor) {
		if (isClosed) return false;
		final boolean acquired = newAssetOperationsLock.tryLock();
		if (acquired) {
			logger.info("Lock acquired. (by '" + actor + "')");
			eventSender.broadcastEvent(WebSocketEvent.AssetFileOperationsChangeEvent);
		}
		return acquired;
	}
	
	/**
	 * Release held lock.
	 * @see {@link ReentrantLock#unlock()}
	 */
	public synchronized void unlock(String actor) {
		newAssetOperationsLock.unlock();
		logger.info("Lock released. (by '" + actor + "')");
		if (!isClosed) {
			eventSender.broadcastEvent(WebSocketEvent.AssetFileOperationsChangeEvent);
		}
	}
	
	/**
	 * Make it impossible for other threads to acquire the lock until opened. Calling thread must own lock.
	 * When calling this, it must be guaranteed that {@link NewAssetLockService#attemptOpenLock()} is called eventually!
	 */
	public synchronized void closeLock(String actor) {
		if (!isClosed) {
			if (!newAssetOperationsLock.isHeldByCurrentThread()) {
				throw new IllegalStateException("Calling thread must still hold lock to close!");
			}
			logger.info("Lock closed. (by '" + actor + "')");
			isClosed = true;
		}
	}
	
	/**
	 * Make it possible for any thread to acquire lock again. Does nothing if lock is still held.
	 */
	public synchronized void attemptOpenLock(String actor) {
		if (!newAssetOperationsLock.isLocked()) {
			if (isClosed) {
				logger.info("Lock opened. (by '" + actor + "')");
				isClosed = false;
				notifyAll();
				eventSender.broadcastEvent(WebSocketEvent.AssetFileOperationsChangeEvent);
			}
		}
	}
	
	/**
	 * @see {@link ReentrantLock#isLocked()}
	 */
	public synchronized boolean isAvailable() {
		return !isClosed && !newAssetOperationsLock.isLocked();
	}
	
	@SuppressWarnings("unused")
	private void testBlock() {
		new Thread(() -> {
			try {
				Thread.sleep(10000);
			} catch (final InterruptedException e) {}
			while(true) {
				lock("");
				try {
					Thread.sleep(5000);
				} catch (final InterruptedException e) {}
				unlock("");
				try {
					Thread.sleep(500);
				} catch (final InterruptedException e) {}
			}
		}).start();;
	}
}
