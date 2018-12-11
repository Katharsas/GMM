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
import gmm.service.data.DataBaseInitNotifier;

@Service
@WebListener
public class BackupExecutorService implements ServletContextListener {
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired private AssetTaskUpdater assetTaskUpdater;
	@Autowired private BackupAccessService backups;
	@Autowired private DataAccess data;
	@Autowired private DataBaseInitNotifier initNotifier;
	
	/**
	 * fixedRate should not influence backup rate
	 */
	@Scheduled(fixedRate = 600000, initialDelay = 600000)
	private void callback() {
		if (initNotifier.isInitDone()) {
			backups.monthlyBackup.execute(true, true, data);
			backups.hourlyBackup.execute(true, true, data);
			backups.daylyBackup.execute(false, true, data);
		}
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
