package gmm.service.data;

import gmm.service.FileService;

import java.nio.file.Path;
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
	
	@Autowired private FileService fileService;
	
	DateTimeFormatter fmt = DateTimeFormat.forPattern("dd-MMM-yyyy'_at_'HH-mm");
	
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
	private void callback() {
		DateTime now = new DateTime();
		
		Hours toHourlyBackup = Hours.hoursBetween(lastHourlyBackup, now);
		if (toHourlyBackup.getHours() >= 1) {
			createHourlyBackup();
			lastHourlyBackup = now;
		}
		Days toDaylyBackup = Days.daysBetween(lastDaylyBackup, now);
		if (toDaylyBackup.getDays() >= 1) {
			createDaylyBackup();
			lastDaylyBackup = now;
		}
		Months toMonthlyBackup = Months.monthsBetween(lastMonthlyBackup, now);
		if (toMonthlyBackup.getMonths() >= 1) {
			createMonthlyBackup();
			lastMonthlyBackup = now;
		}
	}

	private void createHourlyBackup() {
		Path directory = null;
	}
	
	private void createDaylyBackup() {
		
	}
	
	private void createMonthlyBackup() {
		// TODO Auto-generated method stub
	}
	
	private DateTime getDate(String filename, String type) {
		Pattern regex = Pattern.compile("backup_"+type+"_([0-9]{1,2}-[a-zA-Z]{3}-[0-9]{4}_at_[0-9]{1,2}-[0-9]{1,2})\\.xml");
		Matcher matcher = regex.matcher(filename);
		if(!matcher.matches()) {
			throw new IllegalArgumentException("Filename must have form: backup_type_dd-mmm-yyyy_at_hh-mm.xml");
		}
		return fmt.parseDateTime(matcher.group(1));
	}
}
