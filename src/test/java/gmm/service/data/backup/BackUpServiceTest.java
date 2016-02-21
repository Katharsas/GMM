package gmm.service.data.backup;

import static org.junit.Assert.assertEquals;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import gmm.collections.Collection;
import gmm.domain.User;
import gmm.domain.task.Task;
import gmm.service.ajax.MockedResponseBundleHandler;
import gmm.service.ajax.operations.TaskLoaderOperations;
import gmm.service.data.DataAccess;
import gmm.service.data.DataConfigService;
import gmm.service.data.XMLService;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes={gmm.ApplicationConfiguration.class})
public class BackUpServiceTest {
	
	@Autowired private BackupService backupService;
	@Autowired private DataAccess data;
	@Autowired private DataConfigService config;
	@Autowired private XMLService xmlService;
	
	@Test
	public void backupTest() {
		//deserialize tasks/users from test directory
		config.updateWorkspace(Paths.get("WEB-INF/dataTesting"));
		
		final Path tasksFile = config.TASKS.resolve("someGeneralTasks.xml");
		final Collection<Task> tasks = xmlService.deserialize(tasksFile, Task.class);
		final Path usersFile = config.USERS.resolve("someUsers.xml");
		final Collection<User> users = xmlService.deserialize(usersFile, User.class);
		
		//load tasks/users into DataAccess
		final MockedResponseBundleHandler<Task> taskLoader = new MockedResponseBundleHandler<>();
		taskLoader.processResponses(tasks.iterator(), new TaskLoaderOperations());
		data.addAll(users);
		
		//trigger task/user save
		backupService.triggerUserBackup();
		backupService.triggerTaskBackup();
		
		//assert correct tasks backup
		final Path taskSave = backupService.getLatestTaskBackup();
		final Collection<Task> backupedTasks = xmlService.deserialize(taskSave, Task.class);
		final Collection<Task> expectedTasks = data.getList(Task.class);
		assertEquals(expectedTasks, backupedTasks);
		//assert correct users backup
		final Path userSave = backupService.getLatestUserBackup();
		final Collection<User> backupedUsers = xmlService.deserialize(userSave, User.class);
		final Collection<User> expectedUsers = data.getList(User.class);
		assertEquals(expectedUsers, backupedUsers);
	}
}
