package gmm.service.data;

import gmm.domain.GeneralTask;
import gmm.domain.Linkable;
import gmm.domain.User;
import gmm.service.FileService;
import gmm.service.FileService.FileExtensionFilter;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * We need to backup:
 * 
 * Users
 * - on shutdown (one file)
 * - hourly (last 24 h)
 * - dayly (last 7 days)
 * - monthly (last 6 months)
 * 
 * Tasks
 * - on shutdown (one file)
 * - dayly (last 7 days)
 * - monthly (last 6 months)
 * 
 * Saving on change for:
 * Configuration, CombinedData
 * 
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
	
	private static final Path taskBackupPath = Paths.get("/autoBackups");
	private static final Path userBackupPath = Paths.get("/autoBackups");
	private static final Path monthlyPath = Paths.get("/monthly");
	private static final Path daylyPath = Paths.get("/dayly");
	private static final Path hourlyPath = Paths.get("/hourly");
	
	@Autowired private FileService fileService;
	@Autowired private DataConfigService config;
	@Autowired private XMLService serializer;
	@Autowired private DataAccess data;
	
	DateTimeFormatter formatter = DateTimeFormat.forPattern("dd-MMM-yyyy'_at_'HH-mm");
	
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
		directory = config.USERS.resolve(userBackupPath).resolve(hourlyPath);
		createBackUp(now, directory, User.class, 24);
	}
	
	private void createDaylyBackup(DateTime now) throws Exception {
		Path directory;
		directory = config.TASKS.resolve(taskBackupPath).resolve(daylyPath);
		createBackUp(now, directory, GeneralTask.class, 7);
		directory = config.USERS.resolve(userBackupPath).resolve(daylyPath);
		createBackUp(now, directory, User.class, 7);
	}
	
	private void createMonthlyBackup(DateTime now) throws Exception {
		Path directory;
		directory = config.TASKS.resolve(taskBackupPath).resolve(monthlyPath);
		createBackUp(now, directory, GeneralTask.class, 6);
		directory = config.USERS.resolve(userBackupPath).resolve(monthlyPath);
		createBackUp(now, directory, User.class, 6);
	}
	
	private void createBackUp(DateTime timeStamp, Path directory, Class<? extends Linkable> type, int maxSaveFiles) throws Exception {
		//add backup file
		Path save = directory.resolve(getFileName(type.getSimpleName(), timeStamp));
		serializer.serialize(data.getList(type), save);
		//remove files if too many
		File[] files = directory.toFile().listFiles(new FileExtensionFilter(new String[]{"xml"}));
		sortByDate(files, type.getSimpleName());
		List<File> fileList = Arrays.asList(files);
		if (fileList.size() > maxSaveFiles) {
			fileList.remove(0);
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
		Pattern regex = Pattern.compile("backup_"+type+"_([0-9]{1,2}-[a-zA-Z]{3}-[0-9]{4}_at_[0-9]{1,2}-[0-9]{1,2})\\.xml");
		Matcher matcher = regex.matcher(filename);
		if(!matcher.matches()) {
			throw new IllegalArgumentException("Filename must have form: backup_type_dd-mmm-yyyy_at_hh-mm.xml");
		}
		return formatter.parseDateTime(matcher.group(1));
	}
	
	private Path getFileName(String type, DateTime date) {
		return Paths.get("backup_"+type+"_"+formatter.print(date)+".xml");
	}
}
