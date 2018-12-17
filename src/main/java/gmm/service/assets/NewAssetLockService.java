package gmm.service.assets;

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gmm.web.WebSocketEventSender;
import gmm.web.WebSocketEventSender.WebSocketEvent;

/**
 * Manages ownership of code over new asset folder. Combines two locking mechanisms:
 * <br>
 * <br> - R/W Lock:		Used to synchronize normal (non-async), short-lived threads
 * <br> - Fixed Flag:	Used by threads that have read lock before unlocking to "extend" the "lock"
 * 			beyond their own lifetime when they activate worker threads to do preview processing, so
 * 			that other threads cannot acquire the write lock until workers are done with async tasks.
 * 
 * @author Jan Mothes
 */
@Service
public class NewAssetLockService {
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private final WebSocketEventSender eventSender;
	
	private final ReentrantReadWriteLock newAssetOperationsLock = new ReentrantReadWriteLock(true);
	private volatile boolean isReadLockFixed = false; // flag, basically simulates an additional read lock when true
	
	@Autowired
	public NewAssetLockService(WebSocketEventSender eventSender) {
		this.eventSender = eventSender;
//		testWriteLock();
	}
	
	/**
	 * Will block calling until lock is both open and has been acquired.
	 * @see {@link ReadLock#lock()}
	 */
	public synchronized void readLock(String actor) {
		while (!newAssetOperationsLock.readLock().tryLock()) {
			try {
				wait();
			} catch (final InterruptedException e) {}
		}
		logger.debug("Read Lock acquired. (by '" + actor + "')");
		if (newAssetOperationsLock.getReadLockCount() <= 1) {
			eventSender.broadcastEvent(WebSocketEvent.AssetFileOperationsChangeEvent);
		}
	}
	
	/**
	 * Will block calling thread until lock is both open and has been acquired.
	 * @see {@link WriteLock#lock()}
	 */
	public synchronized void writeLock(String actor) {
		while (isReadLockFixed || !newAssetOperationsLock.writeLock().tryLock()) {
			try {
				wait();
			} catch (final InterruptedException e) {}
		}
		logger.debug("Write Lock acquired. (by '" + actor + "')");
		eventSender.broadcastEvent(WebSocketEvent.AssetFileOperationsChangeEvent);
	}
	
	/**
	 * @return True if lock is both open and calling thread could acquire lock.
	 * @see {@link ReadLock#tryLock()}
	 */
	public synchronized boolean tryReadLock(String actor) {
		final boolean acquired = newAssetOperationsLock.readLock().tryLock();
		if (acquired) {
			logger.info("Read Lock acquired. (by '" + actor + "')");
			if (newAssetOperationsLock.getReadLockCount() <= 1) {
				eventSender.broadcastEvent(WebSocketEvent.AssetFileOperationsChangeEvent);
			}
		}
		return acquired;
	}
	
	/**
	 * @return True if lock is both open and calling thread could acquire lock.
	 * @see {@link WriteLock#tryLock()}
	 */
	public synchronized boolean tryWriteLock(String actor) {
		if (isReadLockFixed) return false;
		final boolean acquired = newAssetOperationsLock.writeLock().tryLock();
		if (acquired) {
			logger.info("Write Lock acquired. (by '" + actor + "')");
			eventSender.broadcastEvent(WebSocketEvent.AssetFileOperationsChangeEvent);
		}
		return acquired;
	}
	
	/**
	 * Release held lock.
	 * @see {@link ReadLock#unlock()}
	 */
	public synchronized void readUnlock(String actor) {
		newAssetOperationsLock.readLock().unlock();
		logger.info("Read Lock released. (by '" + actor + "')");
		if (isWriteAvailable()) {
			// only affects availability of write lock
			eventSender.broadcastEvent(WebSocketEvent.AssetFileOperationsChangeEvent);
		}
	}
	
	/**
	 * Release held lock.
	 * @see {@link WriteLock#unlock()}
	 */
	public synchronized void writeUnlock(String actor) {
		newAssetOperationsLock.writeLock().unlock();
		logger.info("Write Lock released. (by '" + actor + "')");
		// affects availability of both read and write locks
		eventSender.broadcastEvent(WebSocketEvent.AssetFileOperationsChangeEvent);
	}
	
	/**
	 * Make it impossible for other threads to acquire write lock until opened. Calling thread must own lock.
	 * When calling this, it must be guaranteed that {@link NewAssetLockService#attemptOpenReadLock()} is called eventually!
	 */
	public synchronized void fixReadLock(String actor) {
		if (newAssetOperationsLock.getReadHoldCount() <= 0) {
			throw new IllegalStateException("Calling thread must still hold read lock to close!");
		}
		if (!isReadLockFixed) {
			logger.info("Flag CLOSED. (by '" + actor + "')");
		}
		isReadLockFixed = true;
	}
	
	/**
	 * Make it possible for any thread to acquire write lock again. Does nothing if read lock is still held by anybody.
	 */
	public synchronized void attemptUnfixReadLock(String actor) {
		if (isReadLockFixed) {
			if (newAssetOperationsLock.getReadLockCount() <= 0) {
				logger.info("Flag OPENED. (by '" + actor + "')");
				isReadLockFixed = false;
				notifyAll();
				eventSender.broadcastEvent(WebSocketEvent.AssetFileOperationsChangeEvent);
			}
		}
	}
	
	/**
	 * @see {@link ReentrantReadWriteLock#isWriteLocked()}
	 */
	public synchronized boolean isReadAvailable() {
		return newAssetOperationsLock.isWriteLocked();
	}
	
	/**
	 * @see {@link ReentrantLock#isLocked()}
	 */
	public synchronized boolean isWriteAvailable() {
		return !isReadLockFixed && newAssetOperationsLock.getReadLockCount() <= 0;
	}
	
	@SuppressWarnings("unused")
	private void testWriteLock() {
		new Thread(() -> {
			try {
				Thread.sleep(10000);
			} catch (final InterruptedException e) {}
			while(true) {
				writeLock("");
				try {
					Thread.sleep(5000);
				} catch (final InterruptedException e) {}
				writeLock("");
				try {
					Thread.sleep(500);
				} catch (final InterruptedException e) {}
			}
		}).start();;
	}
}
