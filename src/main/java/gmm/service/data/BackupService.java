package gmm.service.data;

import gmm.collections.Collection;
import gmm.domain.Linkable;
import gmm.domain.User;
import gmm.domain.task.Task;
import gmm.service.FileService;
import gmm.service.FileService.FileExtensionFilter;

import java.io.File;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Saves names are contructed like:
 * backup_type_dd-mmm-yyyy_at_hh-mm.xml
 * 
 * e.g:
 * backup_tasks_23-Sep-2014_at_22-00.xml
 * 
 * @author Jan Mothes
 *
 */
@Service
public class BackupService {
	
	protected final Path backupPath = Paths.get("autoBackups");
	protected final Path triggeredPath = Paths.get("triggered");
	protected final Path monthlyPath = Paths.get("monthly");
	protected final Path daylyPath = Paths.get("dayly");
	protected final Path hourlyPath = Paths.get("hourly");
	
	private final int maxTriggered = 100;
	private final int maxMonthly = 6;
	private final int maxDayly = 7;
	private final int maxHourly = 24;
	
	@Autowired private FileService fileService;
	@Autowired private DataConfigService config;
	@Autowired private XMLService serializer;
	@Autowired private DataAccess data;
	
	private final FileExtensionFilter xmlFilter = new FileExtensionFilter(new String[]{"xml"});
	private final DateTimeFormatter formatter =  DateTimeFormat.forPattern("yyyy-MMM-dd'_at_'HH-mm-ss")
			.withLocale(Locale.ENGLISH);
	
	private DateTime lastHourlyBackup;
	private DateTime lastDaylyBackup;
	private DateTime lastMonthlyBackup;
	
	public BackupService() {
		lastHourlyBackup = new DateTime();
		lastDaylyBackup = new DateTime();
		lastMonthlyBackup = new DateTime();
	}
	
	/**
	 * fixedRate should not influence backup rate
	 */
	@Scheduled(fixedRate=3500000)
	private void callback() throws Exception {
		DateTime now = new DateTime();
		
		Hours toHourlyBackup = Hours.hoursBetween(lastHourlyBackup, now);
		if (toHourlyBackup.getHours() >= 1) {
			createHourlyBackup(now);
			lastHourlyBackup = now;
		}
		Days toDaylyBackup = Days.daysBetween(lastDaylyBackup, now);
		if (toDaylyBackup.getDays() >= 1) {
			createDaylyBackup(now);
			lastDaylyBackup = now;
		}
		Months toMonthlyBackup = Months.monthsBetween(lastMonthlyBackup, now);
		if (toMonthlyBackup.getMonths() >= 1) {
			createMonthlyBackup(now);
			lastMonthlyBackup = now;
		}
	}

	private void createHourlyBackup(DateTime now) throws Exception {
		Path directory;
		directory = config.USERS.resolve(backupPath).resolve(hourlyPath);
		createBackUp(now, directory, User.class, maxHourly);
	}
	
	private void createDaylyBackup(DateTime now) throws Exception {
		Path directory;
		directory = config.TASKS.resolve(backupPath).resolve(daylyPath);
		createBackUp(now, directory, Task.class, maxDayly);
		directory = config.USERS.resolve(backupPath).resolve(daylyPath);
		createBackUp(now, directory, User.class, maxDayly);
	}
	
	private void createMonthlyBackup(DateTime now) throws Exception {
		Path directory;
		directory = config.TASKS.resolve(backupPath).resolve(monthlyPath);
		createBackUp(now, directory, Task.class, maxMonthly);
		directory = config.USERS.resolve(backupPath).resolve(monthlyPath);
		createBackUp(now, directory, User.class, maxMonthly);
	}
	
	public void triggerUserBackup() throws Exception {
		DateTime now = new DateTime();
		Path directory = config.USERS.resolve(backupPath).resolve(triggeredPath);
		createBackUp(now, directory, User.class, maxTriggered);
	}
	
	public void triggerTaskBackup() throws Exception {
		DateTime now = new DateTime();
		Path directory = config.TASKS.resolve(backupPath).resolve(triggeredPath);
		createBackUp(now, directory, Task.class, maxTriggered);
	}
	
	/**
	 * Only for internal use!
	 */
	@Service
	@WebListener
	public static class BackupServiceShutdownHook implements ServletContextListener {
		@Autowired BackupService service;
		@Override
		public void contextInitialized(ServletContextEvent sce) {}
		@Override
		public void contextDestroyed(ServletContextEvent sce) {
			try {
				service.triggerUserBackup();
				service.triggerTaskBackup();
			}
			catch (Exception e) {throw new IllegalStateException(e);}
		}
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
	
	public Path getLatestBackup(Path folder, String type) {
		List<File> files = asList(folder.resolve(triggeredPath));
		files.addAll(asList(folder.resolve(daylyPath)));
		files.addAll(asList(folder.resolve(hourlyPath)));
		files.addAll(asList(folder.resolve(daylyPath)));
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
	
	private void createBackUp(DateTime timeStamp, Path directory, Class<? extends Linkable> type, int maxSaveFiles) throws Exception {
		//add backup file
		Collection<? extends Linkable> toSave = data.getList(type);
		if(toSave.size() > 0) {
			Path path = directory.resolve(getFileName(type.getSimpleName(), timeStamp));
			serializer.serialize(toSave, path);
			//remove files if too many
			File[] files = directory.toFile().listFiles(xmlFilter);
			sortByDate(files, type.getSimpleName());
			if (files.length > maxSaveFiles) {
				fileService.delete(files[0].toPath());
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
