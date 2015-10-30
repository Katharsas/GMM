package gmm.service.data.backup;

import gmm.collections.Collection;
import gmm.domain.Linkable;
import gmm.domain.User;
import gmm.domain.task.Task;
import gmm.service.FileService;
import gmm.service.FileService.FileExtensionFilter;
import gmm.service.data.DataAccess;
import gmm.service.data.DataConfigService;
import gmm.service.data.XMLService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Hours;
import org.joda.time.Months;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.context.support.WebApplicationContextUtils;

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
				Path directory = config.TASKS.resolve(backupPath).resolve(subDir);
				createBackUp(now, directory, Task.class, maxBackups);
			}
			if (saveUsers) {
				Path directory = config.USERS.resolve(backupPath).resolve(subDir);
				createBackUp(now, directory, User.class, maxBackups);
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
	
	@Autowired private FileService fileService;
	@Autowired private DataConfigService config;
	@Autowired private XMLService serializer;
	@Autowired private DataAccess data;
	
	protected final Path backupPath = Paths.get("autoBackups");
	
	private final ConditionalBackupExecutor monthlyBackup;
	private final ConditionalBackupExecutor daylyBackup;
	private final ConditionalBackupExecutor hourlyBackup;
	private final BackupExecutor triggeredBackup;
	
	private final FileExtensionFilter xmlFilter = new FileExtensionFilter(new String[]{"xml"});
	private final DateTimeFormatter formatter =  DateTimeFormat.forPattern("yyyy-MMM-dd'_at_'HH-mm-ss")
			.withLocale(Locale.ENGLISH);
	
	
	public BackupService() {
		
		// timed backups
		monthlyBackup = new ConditionalBackupExecutor("monthly", 6, now -> {
				Months duration = Months.monthsBetween(BackupService.this.monthlyBackup.last(), now);
				return duration.getMonths() >= 1;
			}
		);
		daylyBackup = new ConditionalBackupExecutor("dayly", 7, now -> {
				Days duration = Days.daysBetween(BackupService.this.daylyBackup.last(), now);
				return duration.getDays() >= 1;
			}
		);
		hourlyBackup = new ConditionalBackupExecutor("hourly", 24, now -> {
				Hours duration = Hours.hoursBetween(BackupService.this.hourlyBackup.last(), now);
				return duration.getHours() >= 1;
			}
		);
		// triggered backups
		triggeredBackup = new BackupExecutor("triggered", 100);
	}
	
	/**
	 * fixedRate should not influence backup rate
	 */
	@Scheduled(fixedRate=3500000)
	private void callback() throws Exception {
		DateTime now = new DateTime();
		monthlyBackup.execute(now, true, true);
		hourlyBackup.execute(now, true, true);
		daylyBackup.execute(now, false, true);
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
		catch (Exception e) {
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
		String type = User.class.getSimpleName();
		Path folder = config.USERS.resolve(backupPath);
		return getLatestBackup(folder, type);
	}
	
	public Path getLatestTaskBackup() {
		String type = Task.class.getSimpleName();
		Path folder = config.TASKS.resolve(backupPath);
		return getLatestBackup(folder, type);
	}
	
	private Path getLatestBackup(Path folder, String type) {
		List<File> files = asList(folder.resolve(triggeredBackup.getSubDir()));
		files.addAll(asList(folder.resolve(monthlyBackup.getSubDir())));
		files.addAll(asList(folder.resolve(hourlyBackup.getSubDir())));
		files.addAll(asList(folder.resolve(daylyBackup.getSubDir())));
		File[] fileArray = files.toArray(new File[files.size()]);
		sortByDate(fileArray, type);
		return fileArray[fileArray.length-1].toPath();
	}
	
	private List<File> asList(Path folder) {
		File[] files = folder.toFile().listFiles(xmlFilter);
		if (files == null) {
			files = new File[] {};
		}
		return Arrays.asList(files);
	}
	
	/**
	 * Save backup to a folder. All xml files in that folder must also be
	 * previously created backup files.
	 * 
	 * @param timeStamp - Determines time of backup / filename, usually now.
	 * @param directory - Target directory for backup file.
	 * @param type - Type to use to get data for backup from DataAccess.
	 * @param maxSaveFiles - max number of xml files in this folder. If this 
	 * 		backup would cause the files to exceed this number, the oldest backup
	 * 		will be deleted. All files in the directory with ".xml" ending are
	 * 		expected to be valid backup files with expected file name pattern!
	 */
	private void createBackUp(DateTime timeStamp, Path directory, Class<? extends Linkable> type, int maxSaveFiles) {
		//add backup file
		Collection<? extends Linkable> toSave = data.getList(type);
		if(toSave.size() > 0) {
			Path path = directory.resolve(getFileName(type.getSimpleName(), timeStamp));
			try {
				logger.info("Creating backup for type '" + type.getSimpleName() + "' at " + path);
				serializer.serialize(toSave, path);
				//remove files if too many
				File[] files = directory.toFile().listFiles(xmlFilter);
				sortByDate(files, type.getSimpleName());
				if (files.length > maxSaveFiles) {
					fileService.delete(files[0].toPath());
				}
			} catch (IOException e) {
				logger.error("Failed to create backup for type '" + type.getSimpleName() + "' at " + path, e);
			}
		}
	}
	
	private void sortByDate(File[] files, final String type) {
		Arrays.sort(files, new Comparator<File>() {
			@Override
			public int compare(File o1, File o2) {
				DateTime d1 = getDate(o1.getName(), type);
				DateTime d2 = getDate(o2.getName(), type);
				return d1.compareTo(d2);
			}
		});
	}
	
	private DateTime getDate(String filename, String type) {
		Pattern regex = Pattern.compile("backup_"+type+"_([0-9]{4}-[a-zA-Z]{3}-[0-9]{1,2}_at_[0-9]{1,2}-[0-9]{2}-[0-9]{2})\\.xml");
		Matcher matcher = regex.matcher(filename);
		if(!matcher.matches()) {
			throw new IllegalArgumentException("Filename must have form: "
					+ "backup_type_yyyy-mmm-dd_at_hh-mm-ss.xml, but is " + filename
					+", type must be "+type);
		}
		return formatter.parseDateTime(matcher.group(1));
	}
	
	private Path getFileName(String type, DateTime date) {
		return Paths.get("backup_"+type+"_"+formatter.print(date)+".xml");
	}
}
