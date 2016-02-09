package gmm.service.data.backup;

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Hours;
import org.joda.time.Months;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.context.support.WebApplicationContextUtils;

import gmm.domain.Linkable;
import gmm.domain.User;
import gmm.domain.task.Task;
import gmm.service.data.DataConfigService;

/**
 * Saves names are contructed like:
 * backup_type_dd-mmm-yyyy_at_hh-mm.xml
 * 
 * e.g:
 * backup_tasks_23-Sep-2014_at_22-00.xml
 * 
 * @author Jan Mothes
 */
@Service
@WebListener
public class BackupService implements ServletContextListener {
	
	/*
	 * ###############################
	 * Inner classes / interfaces
	 * ###############################
	 */
	
	/**
	 * Custom boolean supplier functional interface.
	 */
	private interface BoolSupplier {public boolean get(DateTime now);}
	
	/**
	 * Creates backups in a certain subfolder when executed.
	 * Starts deleting old backups when there are maxBackups backups.
	 */
	private class BackupExecutor {
		
		private final Path subDir;
		private final int maxBackups;
		private DateTime last;
		
		public BackupExecutor(String subDir, int maxBackups) {
			this.subDir = Paths.get(subDir);
			this.maxBackups = maxBackups;
			this.last = DateTime.now();
		}
		/**
		 * @param now - provide current DateTime to reduce unnecessary instancing
		 * @param saveTasks - true if this exeuctor should backup all tasks
		 * @param saveUsers - true if this executor should backup all users
		 */
		public void execute(DateTime now, boolean saveTasks, boolean saveUsers) {
			last = now;
			if (saveTasks) {
				final Path directory = config.TASKS.resolve(backupPath).resolve(subDir);
				service.createBackUp(now, directory, Task.class, maxBackups);
			}
			if (saveUsers) {
				final Path directory = config.USERS.resolve(backupPath).resolve(subDir);
				service.createBackUp(now, directory, User.class, maxBackups);
			}
		}
		public Path getSubDir() {return subDir;}
		public DateTime last() {return last;}
	}
	
	/**
	 * Creates backups when executed if the supplied condition evaluates to true.
	 */
	private class ConditionalBackupExecutor extends BackupExecutor {
		private final BoolSupplier condition;
		 ConditionalBackupExecutor(String subDir, int maxBackups, BoolSupplier condition) {
			 super(subDir, maxBackups);
			 this.condition = condition;
		 }
		 @Override
		 public void execute(DateTime now, boolean saveTasks, boolean saveUsers) {
			if(condition.get(now)) super.execute(now, saveTasks, saveUsers);
		 }
	}
	
	/*
	 * ###############################
	 * Members
	 * ###############################
	 */
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	
	@Autowired private DataConfigService config;
	@Autowired private BackupFileService service;
	
	
	protected final Path backupPath = Paths.get("autoBackups");
	
	private final ConditionalBackupExecutor monthlyBackup;
	private final ConditionalBackupExecutor daylyBackup;
	private final ConditionalBackupExecutor hourlyBackup;
	private final BackupExecutor triggeredBackup;
	

	
	
	public BackupService() {
		
		// timed backups
		monthlyBackup = new ConditionalBackupExecutor("monthly", 6, now -> {
				final Months duration = Months.monthsBetween(BackupService.this.monthlyBackup.last(), now);
				return duration.getMonths() >= 1;
			}
		);
		daylyBackup = new ConditionalBackupExecutor("dayly", 7, now -> {
				final Days duration = Days.daysBetween(BackupService.this.daylyBackup.last(), now);
				return duration.getDays() >= 1;
			}
		);
		hourlyBackup = new ConditionalBackupExecutor("hourly", 24, now -> {
				final Hours duration = Hours.hoursBetween(BackupService.this.hourlyBackup.last(), now);
				return duration.getHours() >= 1;
			}
		);
		// triggered backups
		triggeredBackup = new BackupExecutor("triggered", 100);
	}
	
	/**
	 * fixedRate should not influence backup rate
	 */
	@Scheduled(fixedRate=600000)
	private void callback() {
		final DateTime now = new DateTime();
		monthlyBackup.execute(now, true, true);
		hourlyBackup.execute(now, true, true);
		daylyBackup.execute(now, false, true);
		throw new IllegalStateException("Test");
	}
	
	@Override
	public void contextInitialized(ServletContextEvent sce) {
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		//Spring is not active anymore
		// => exceptions must be catched manually, DI must be invoked (autowiring)
		try {
			WebApplicationContextUtils
	        .getRequiredWebApplicationContext(sce.getServletContext())
	        .getAutowireCapableBeanFactory()
	        .autowireBean(this);
			triggeredBackup.execute(DateTime.now(), true, true);
		}
		catch (final Exception e) {
			logger.error(e.getMessage(), e);;
		}
	}
	
	public void triggerTaskBackup() {
		triggeredBackup.execute(DateTime.now(), true, false);
	}
	
	public void triggerUserBackup() {
		triggeredBackup.execute(DateTime.now(), false, true);
	}
	
	public Path getLatestUserBackup() {
		final Path folder = config.USERS.resolve(backupPath);
		return getLatestBackup(folder, User.class);
	}
	
	public Path getLatestTaskBackup() {
		final Path folder = config.TASKS.resolve(backupPath);
		return getLatestBackup(folder, Task.class);
	}
	
	private Path getLatestBackup(Path folder, Class<? extends Linkable> type) {
		return service.getLatestBackup(
				type,
				triggeredBackup.getSubDir(),
				monthlyBackup.getSubDir(),
				daylyBackup.getSubDir(),
				hourlyBackup.getSubDir()
				);
	}
}
