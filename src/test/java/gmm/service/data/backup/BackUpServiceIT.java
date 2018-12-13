package gmm.service.data.backup;

import static org.junit.Assert.assertEquals;

import java.nio.file.Path;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import gmm.IntegrationTest;
import gmm.collections.Collection;
import gmm.domain.User;
import gmm.domain.task.Task;
import gmm.service.data.DataAccess;
import gmm.service.data.PathConfig;
import gmm.service.data.xstream.XMLService;

@Category(IntegrationTest.class)
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes={
		gmm.ApplicationConfiguration.class,
		gmm.SecurityConfiguration.class})
public class BackupServiceIT {
	
	private static PathConfig config;
	
	@Autowired private BackupAccessService backupAccess;
	@Autowired private BackupExecutorService backupExecutor;
	@Autowired private DataAccess data;
	@Autowired private XMLService xmlService;
	
	// TODO use test config with autoload/defaultUser disabled!
	
	@BeforeClass
	public static void init() {
		config = new MockDataConfigService();// TODO set paths / load mock config
	}
	
	@Test
	public void backupTestTasks() {
		final Path tasksFile = config.dbTasks().resolve("someGeneralTasks.xml");
		final Collection<Task> tasks = xmlService.deserializeAll(tasksFile, Task.class);
		
		backupExecutor.triggerUserBackup();
		backupExecutor.triggerTaskBackup(true);
		
		//assert correct tasks backup
		final Collection<Task> backupedTasks = backupAccess.getLatestTaskBackup();
		final Collection<Task> expectedTasks = data.getList(Task.class);
		assertEquals(expectedTasks, backupedTasks);
	}
	
	@Test
	public void backupTestusers() {
		final Path usersFile = config.dbUsers().resolve("someUsers.xml");
		final Collection<User> users = xmlService.deserializeAll(usersFile, User.class);
		
		backupExecutor.triggerUserBackup();
		backupExecutor.triggerTaskBackup(true);
		
		//assert correct users backup
		final Collection<User> backupedUsers = backupAccess.getLatestUserBackup();
		final Collection<User> expectedUsers = data.getList(User.class);
		assertEquals(expectedUsers, backupedUsers);
	}
}
