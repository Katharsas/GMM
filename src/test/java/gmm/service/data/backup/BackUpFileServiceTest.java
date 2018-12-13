package gmm.service.data.backup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import gmm.TestConfig;
import gmm.service.FileService;
import gmm.service.data.DataBaseInitNotifier;
import gmm.service.data.MockDataBaseInitNotifier;
import gmm.service.data.MockPersistanceService;
import gmm.service.data.PersistenceService;

public class BackupFileServiceTest {
	
	private static Path testPath;
	
	private static PersistenceService xmlService;
	private static BackupFileService backupFileService;
	private static DataBaseInitNotifier initNotifier;
	
	@BeforeClass
	public static void init() {
		testPath = TestConfig.getTestFolderPath(BackupFileServiceTest.class);
		TestConfig.createTestFolder(testPath);
		
		final FileService fileService = new FileService();
		xmlService = new MockPersistanceService(true);
		initNotifier = new MockDataBaseInitNotifier();
		backupFileService = new BackupFileService(fileService, xmlService, initNotifier);
	}
	
	@AfterClass
	public static void clean() {
		TestConfig.deleteTestFolder(testPath);
	}
	
	// TODO test what happens, when files with invalid date or format in general exist
	
	@Before
	private void setUp() throws IOException {
		final String[] fileNames = {
			"backup_CombinedData_2016-Jun-20_at_00-14-00",
			"backup_CombinedData_2016-Jul-19_at_00-20-00",
			// TODO other types
		};
		for (final String fileName : fileNames) {
			Files.createFile(testPath.resolve(fileName));
		}
	}
	
	@After
	private void tearDown() {
		// remove test backups files
	}
	
	@Test
	public void testCreateBackup() {
		// TODO test creating a backup
		// TODO test automatic removal of backup when over maxSaveFiles
	}
	
	@Test
	public void testRemoveOldestBackup() {
		// TODO create backup, manipulate fileName date, test removing it
	}
	
	@Test
	public void testGetFromLatest() {
		// TODO create multiple backups with different data, test if get most recent one
	}
}
