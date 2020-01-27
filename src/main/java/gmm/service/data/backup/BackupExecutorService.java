package gmm.service.data.backup;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import gmm.service.assets.AssetTaskUpdater;
import gmm.service.data.DataAccess;

@Service
public class BackupExecutorService implements DisposableBean {
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private final AssetTaskUpdater assetTaskUpdater;
	private final BackupAccessService backups;
	private final DataAccess data;
	
	@Autowired
	public BackupExecutorService(AssetTaskUpdater assetTaskUpdater, BackupAccessService backups, DataAccess data) {
		this.assetTaskUpdater = assetTaskUpdater;
		this.backups = backups;
		this.data = data;
	}

	/**
	 * Initial delay to test things in development without persisting
	 */
	@Scheduled(fixedRate = 1 * 60 * 1000, initialDelay = 5 * 60 * 1000)
	private void callback() {
		backups.monthlyBackup.execute(true, true, data);
		backups.hourlyBackup.execute(true, true, data);
		backups.daylyBackup.execute(false, true, data);
	}
	
	@Override
	public void destroy() {
		backups.triggeredBackup.execute(true, true, data);
	}
	
	public void triggerTaskBackup(boolean blockUntilDone) {
		final CompletableFuture<Void> done = assetTaskUpdater.allAyncTaskProcessing().thenRun(()->{
			backups.triggeredBackup.execute(true, false, data);
		});
		if (!done.isDone()) {
			logger.debug("Waiting for AssetTaskUpdater to finish before creating backup...");
		}
		if (blockUntilDone) {
			done.join();
		}
	}
	
	public void triggerUserBackup() {
		backups.triggeredBackup.execute(false, true, data);
	}
}
