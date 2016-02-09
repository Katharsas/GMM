package gmm.service.data.backup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Locale;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gmm.collections.Collection;
import gmm.domain.Linkable;
import gmm.service.FileService;
import gmm.service.FileService.FileExtensionFilter;
import gmm.service.data.DataAccess;
import gmm.service.data.XMLService;

/**
 * Lower level service for BackupService.
 * Wraps all occuring exceptions into RunTimeExceptions.
 * 
 * @author Jan Mothes
 */
@Service
public class BackupFileService {
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired private FileService fileService;
	@Autowired private XMLService serializer;
	@Autowired private DataAccess data;
	
	private final FileExtensionFilter xmlFilter = new FileExtensionFilter(new String[]{"xml"});
	private final DateTimeFormatter formatter =  DateTimeFormat.forPattern("yyyy-MMM-dd'_at_'HH-mm-ss")
			.withLocale(Locale.ENGLISH);
	
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
	protected void createBackUp(DateTime timeStamp, Path directory, Class<? extends Linkable> type, int maxSaveFiles) {
		//add backup file
		final Collection<? extends Linkable> toSave = data.getList(type);
		if(toSave.size() > 0) {
			final Path path = directory.resolve(getFileName(type.getSimpleName(), timeStamp));
			try {
				logger.info("Creating backup for type '" + type.getSimpleName() + "' at " + path);
				serializer.serialize(toSave, path);
				removeOldestBackup(type, directory, maxSaveFiles);
			} catch (final IOException e) {
				throw new BackupServiceException("Failed to create backup for type '"
						+ type.getSimpleName() + "' at " + path + "'!", e);
			}
		}
	}
	
	protected void removeOldestBackup(Class<? extends Linkable> type, Path parent, int maxSaveFiles) {
		final TreeSet<Path> paths = new TreeSet<>(new BackupFileComparator(type.getSimpleName()));
		try {
			try(Stream<Path> dir = Files.list(parent)) {
				dir
					.filter(Files::isRegularFile)
					.filter(xmlFilter)
					.collect(Collectors.toCollection(()->paths));
			}
			if (paths.size() > maxSaveFiles) {
				fileService.delete(paths.first());
			}
		} catch (final IOException e) {
			throw new BackupServiceException("Failed to remove oldest from folder '"
					+ parent.toString() + "'!", e);
		}
	}
	
	/**
	 * @param type - Type that was used to create backups in specified parent directories.
	 * @param parents - Directories directly containing backup files of specified type (only!).
	 * @return Path to backup file that was created most recently.
	 */
	protected Path getLatestBackup(Class<? extends Linkable> type, Path... parents) {
		final TreeSet<Path> paths = new TreeSet<>(new BackupFileComparator(type.getSimpleName()));
		for (final Path parent : parents) {
			try(Stream<Path> dir = Files.list(parent)) {
				dir
					.filter(Files::isRegularFile)
					.filter(xmlFilter)
					.collect(Collectors.toCollection(()->paths));
			} catch (final IOException e) {
				throw new BackupServiceException("Failed to get latest backup from folder '"
						+ parent.toString() + "'!", e);
			}
		}
		return paths.last();
	}
	
	private class BackupFileComparator implements Comparator<Path> {
		String customNamePart;
		public BackupFileComparator(String customNamePart) {
			this.customNamePart = customNamePart;
		}
		@Override
		public int compare(Path o1, Path o2) {
			final DateTime d1 = getDate(o1.getFileName().toString(), customNamePart);
			final DateTime d2 = getDate(o2.getFileName().toString(), customNamePart);
			return d1.compareTo(d2);
		}
	}
	
	protected DateTime getDate(String filename, String type) {
		final Pattern regex = Pattern.compile("backup_"+type+"_([0-9]{4}-[a-zA-Z]{3}-[0-9]{1,2}_at_[0-9]{1,2}-[0-9]{2}-[0-9]{2})\\.xml");
		final Matcher matcher = regex.matcher(filename);
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
	
	protected static class BackupServiceException extends RuntimeException {
		private static final long serialVersionUID = 4794748930291072744L;
		public BackupServiceException() {
			super();
		}
		public BackupServiceException(String message, Throwable cause, boolean enableSuppression,
				boolean writableStackTrace) {
			super(message, cause, enableSuppression, writableStackTrace);
		}
		public BackupServiceException(String message, Throwable cause) {
			super(message, cause);
		}
		public BackupServiceException(String message) {
			super(message);
		}
		public BackupServiceException(Throwable cause) {
			super(cause);
		}
	}
}
