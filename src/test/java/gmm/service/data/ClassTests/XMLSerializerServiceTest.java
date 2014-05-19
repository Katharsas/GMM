package gmm.service.data.ClassTests;

import static org.junit.Assert.*;

import java.nio.file.Path;
import java.nio.file.Paths;

import gmm.domain.Notification;
import gmm.domain.User;
import gmm.service.data.DataAccess;
import gmm.service.data.DataConfigService;
import gmm.service.data.XMLService;
import gmm.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="XMLSerializerServiceTest-Context.xml")
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
	public void testUserSerialisation() {		
		//add test user
		User testUser = new User("testUser");
		testUser.setPasswordHash("testPasswordHash");
		testUser.setEmail("testEmail");
		testUser.getNewNotifications().add(new Notification("testNotification1"));
		testUser.getOldNotifications().add(new Notification("testNotification2"));
		data.add(testUser);
		
		//serialize and deserialize
		Path filename = Paths.get("user_test_file");
		Collection<User> users = data.<User>getList(User.class);
		xmlService.serialize(users, filename);
		Collection<? extends User> resultUsers = xmlService.deserialize(filename, User.class);
		
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
