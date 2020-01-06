package gmm.service.data.backup;

import java.util.concurrent.CompletableFuture;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.context.support.WebApplicationContextUtils;

import gmm.service.assets.AssetTaskUpdater;
import gmm.service.data.DataAccess;

@Service
@WebListener
public class BackupExecutorService implements ServletContextListener {
	
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
	public void contextInitialized(ServletContextEvent sce) {
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		// Spring is not active anymore
		// => exceptions must be caught manually, DI must be invoked (autowiring)
		try {
			WebApplicationContextUtils
	        .getRequiredWebApplicationContext(sce.getServletContext())
	        .getAutowireCapableBeanFactory()
	        .autowireBean(this);
			backups.triggeredBackup.execute(true, true, data);
		}
		catch (final Exception e) {
			logger.error(e.getMessage(), e);;
		}
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
