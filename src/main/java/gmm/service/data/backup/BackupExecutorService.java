package gmm.service.data.backup;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.context.support.WebApplicationContextUtils;

import gmm.service.data.DataAccess;

@Service
@WebListener
public class BackupExecutorService implements ServletContextListener {
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired private BackupAccessService backups;
	@Autowired private DataAccess data;
	
	/**
	 * fixedRate should not influence backup rate
	 */
	@Scheduled(fixedRate=600000)
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
	
	public void triggerTaskBackup() {
		backups.triggeredBackup.execute(true, false, data);
	}
	
	public void triggerUserBackup() {
		backups.triggeredBackup.execute(false, true, data);
	}
}
