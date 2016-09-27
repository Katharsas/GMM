package gmm.service.data.backup;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Hours;
import org.joda.time.Months;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gmm.collections.Collection;
import gmm.domain.Linkable;
import gmm.domain.User;
import gmm.domain.task.Task;
import gmm.service.data.CombinedData;
import gmm.service.data.DataAccess;
import gmm.service.data.DataConfigService;

/**
 * Saves names are constructed like:
 * backup_&lt;type&gt;_dd-mmm-yyyy_at_hh-mm.xml
 * 
 * e.g:
 * backup_tasks_23-Sep-2014_at_22-00.xml
 * 
 * @author Jan Mothes
 */
@Service
public class BackupAccessService {
	
	/*
	 * ###############################
	 * Inner classes / interfaces
	 * ###############################
	 */
	
	/**
	 * Custom boolean supplier functional interface.
	 */
	@FunctionalInterface
	private interface TimeCondition {public boolean get(DateTime now, DateTime last);}
	
	/**
	 * Creates backups in a certain subfolder when executed.
	 * Starts deleting old backups when there are maxBackups backups.
	 */
	protected class BackupExecutor {
		
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
		 * @param saveTasks - true if this executor should backup all tasks
		 * @param saveUsers - true if this executor should backup all users
		 */
		public void execute(boolean saveTasks, boolean saveUsers, DataAccess data) {
			last = new DateTime();
			if (saveTasks) {
				
				final Path directory = config.dbTasks().resolve(backupPath).resolve(subDir);
				fileService.createBackup(directory, data.getList(Task.class), maxBackups);
			}
			if (saveUsers) {
				final Path directory = config.dbUsers().resolve(backupPath).resolve(subDir);
				fileService.createBackup(directory, data.getList(User.class), maxBackups);
				// just assume we want backups for combinedData as often as for users
				fileService.createBackup(config.dbOther(), data.getCombinedData(), maxBackups);
			}
		}
		public Path getSubDir() {return subDir;}
		public DateTime last() {return last;}
	}
	
	/**
	 * Creates backups when executed if the supplied condition evaluates to true.
	 */
	protected class ConditionalBackupExecutor extends BackupExecutor {
		
		private final TimeCondition condition;
		
		ConditionalBackupExecutor(String subDir, int maxBackups, TimeCondition condition) {
			super(subDir, maxBackups);
			this.condition = condition;
		}
		@Override
		public void execute(boolean saveTasks, boolean saveUsers, DataAccess data) {
			final DateTime now = new DateTime();
			if(condition.get(now, last())) super.execute(saveTasks, saveUsers, data);
		}
	}
	
	/*
	 * ###############################
	 * Members
	 * ###############################
	 */
	
	@SuppressWarnings("unused")
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private final DataConfigService config;
	private final BackupFileService fileService;
	
	private final Path backupPath = Paths.get("autoBackups");
	
	protected final ConditionalBackupExecutor monthlyBackup;
	protected final ConditionalBackupExecutor daylyBackup;
	protected final ConditionalBackupExecutor hourlyBackup;
	protected final BackupExecutor triggeredBackup;
	
	@Autowired
	public BackupAccessService(DataConfigService config, BackupFileService service) {
		this.config = config;
		this.fileService = service;
		
		// timed backups
		monthlyBackup = new ConditionalBackupExecutor("monthly", 6, (now, last) -> {
				final Months duration = Months.monthsBetween(last, now);
				return duration.getMonths() >= 1;
			}
		);
		daylyBackup = new ConditionalBackupExecutor("dayly", 7, (now, last) -> {
				final Days duration = Days.daysBetween(last, now);
				return duration.getDays() >= 1;
			}
		);
		hourlyBackup = new ConditionalBackupExecutor("hourly", 24, (now, last) -> {
				final Hours duration = Hours.hoursBetween(last, now);
				return duration.getHours() >= 1;
			}
		);
		// triggered backups
		triggeredBackup = new BackupExecutor("triggered", 100);
	}
	
	public Collection<User> getLatestUserBackup() {
		final Path folder = config.dbUsers().resolve(backupPath);
		return getLatestListBackup(folder, User.class);
	}
	
	public Collection<Task> getLatestTaskBackup() {
		final Path folder = config.dbTasks().resolve(backupPath);
		return getLatestListBackup(folder, Task.class);
	}
	
	public CombinedData getLatestCombinedDataBackup() {
		final Path folder = config.dbOther();
		return fileService.getFromLatestObjectBackup(CombinedData.class, folder);
	}
	
	private <T extends Linkable> Collection<T> getLatestListBackup(Path folder, Class<T> type) {
		return fileService.getFromLatestListBackup(
				type,
				folder.resolve(triggeredBackup.getSubDir()),
				folder.resolve(monthlyBackup.getSubDir()),
				folder.resolve(daylyBackup.getSubDir()),
				folder.resolve(hourlyBackup.getSubDir())
				);
	}
}
