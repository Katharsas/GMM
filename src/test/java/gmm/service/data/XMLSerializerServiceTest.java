package gmm.service.data;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Path;

import gmm.collections.Collection;
import gmm.domain.Notification;
import gmm.domain.User;
import gmm.service.data.DataAccess;
import gmm.service.data.DataConfigService;
import gmm.service.data.XMLService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;


@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes={gmm.ApplicationConfiguration.class})
public class XMLSerializerServiceTest {

	@Autowired
	DataAccess data;
	@Autowired
	XMLService xmlService;
	@Autowired
	DataConfigService dataConfig;
	
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
	
	@Test
	public void testUserSerialisation() throws IOException {		
		//add test user
		User testUser = new User("testUser");
		testUser.setPasswordHash("testPasswordHash");
		testUser.setEmail("testEmail");
		testUser.getNewNotifications().add(new Notification("testNotification1"));
		testUser.getOldNotifications().add(new Notification("testNotification2"));
		data.add(testUser);
		
		//serialize and deserialize
		Path file = dataConfig.USERS.resolve("user_test_file");
		Collection<User> users = data.<User>getList(User.class);
		xmlService.serialize(users, file);
		Collection<? extends User> resultUsers = xmlService.deserialize(file, User.class);
		
		//compare
		for (User u : users) {
			User result = User.getFromName(resultUsers, u.getName());
			assertEquals(u.getCreationDate(), result.getCreationDate());
			assertEquals(u.getEmail(), result.getEmail());
			assertEquals(u.getPasswordHash(), result.getPasswordHash());
			assertEquals(u.getNewNotifications(), result.getNewNotifications());
			assertEquals(u.getNewNotifications(), result.getNewNotifications());
		}
		//remove test user
		data.remove(testUser);
	}
}
