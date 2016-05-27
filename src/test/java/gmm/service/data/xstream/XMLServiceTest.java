package gmm.service.data.xstream;

import static org.junit.Assert.assertEquals;

import java.nio.file.Path;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import gmm.TestConfig;
import gmm.collections.Collection;
import gmm.collections.LinkedList;
import gmm.domain.AssertEquals;
import gmm.domain.Notification;
import gmm.domain.User;
import gmm.domain.task.GeneralTask;
import gmm.domain.task.TaskPriority;
import gmm.domain.task.TaskStatus;
import gmm.service.FileService;
import gmm.service.data.CombinedData;
import gmm.util.Util;

/**
 * TODO: Mock filesystem/fileservice (use memory instead).
 * Maybe create MockFileService with map Path->File to simulate files
 * 
 * @author Jan Mothes
 */
public class XMLServiceTest {

	private static XMLService xmlService;
	private static FileService fileService;
	
	private static Collection<User> users;
	
	private static Path testFolder =
			TestConfig.getTestFolderPath(XMLServiceTest.class);
	
	@BeforeClass
	public static void init() {
		fileService = new FileService();
		xmlService = new XMLService(fileService, () -> users);
		users = new LinkedList<>(User.class);
	}
	
	@Before
	public void setUp() throws Exception {
		fileService.createDirectory(testFolder);
	}

	@After
	public void tearDown() throws Exception {
		fileService.delete(testFolder);
		users.clear();
		
	}
	
	private User createTestUser(String suffix) {
		User testUser = new User("testUser" + suffix);
		testUser.setPasswordHash("testPasswordHash" + suffix);
		testUser.setEmail("testEmail" + suffix + "@test.org");
		testUser.getNewNotifications().add(new Notification("testNotification" + suffix + "_1"));
		testUser.getOldNotifications().add(new Notification("testNotification" + suffix + "_2"));
		return testUser;
	}
	
	@Test
	public void testUserSerialization() {		
		//add test users
		Collection<User> users = new LinkedList<>(User.class);
		users.add(createTestUser("1"));
		users.add(createTestUser("2"));
		users.add(createTestUser("3"));
		
		//serialize and deserialize
		Path file = testFolder.resolve("sub/user_test_file.xml");
		xmlService.serialize(users, file);
		Collection<User> resultUsers = xmlService.deserializeAll(file, User.class);
		
		//compare
		Util.zip(users, resultUsers, (expectedUser, resultUser)
				-> AssertEquals.assertEqualsUser(expectedUser, resultUser));
	}
	
	@Test
	public void testGeneralTaskSerialization() {		
		//add referenced users
		User author = createTestUser("Author");
		User assigned = createTestUser("Assigned");
		users.add(author);
		users.add(assigned);
		
		// setup test task
		GeneralTask task = new GeneralTask(author);
		task.setAssigned(assigned);
		task.setName("TestName");
		task.setLabel("TestLabel");
		task.setDetails("TestDetails");
		task.setPriority(TaskPriority.LOW);
		task.setTaskStatus(TaskStatus.INPROGRESS);
		
		//serialize and deserialize
		Path file = testFolder.resolve("sub/task_test_file.xml");
		xmlService.serialize(new LinkedList<>(GeneralTask.class, task), file);
		GeneralTask result = xmlService.deserializeAll(file, GeneralTask.class).iterator().next();
		
		//compare
		AssertEquals.assertEqualsGeneralTask(task, result);
	}
	
	@Test
	public void testCombinedDataSerialization() {
		CombinedData data = new CombinedData();
		data.setCustomAdminBannerActive(true);
		data.setCustomAdminBanner("<center>Hello World!</center>");
		
		Path file = testFolder.resolve("sub/combinedData_test_file.xml");
		xmlService.serialize(data, file);
		CombinedData result = xmlService.deserialize(file, CombinedData.class);
		
		assertEquals(data.isCustomAdminBannerActive(), result.isCustomAdminBannerActive());
		assertEquals(data.getCustomAdminBanner(), result.getCustomAdminBanner());
	}
}
