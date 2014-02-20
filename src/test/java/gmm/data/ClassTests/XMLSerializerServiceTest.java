package gmm.data.ClassTests;

import static org.junit.Assert.*;

import gmm.domain.Notification;
import gmm.domain.User;
import gmm.service.data.DataAccess;
import gmm.service.data.DataConfigService;
import gmm.service.data.XMLSerializerService;
import gmm.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="GMM-servlet.xml")
public class XMLSerializerServiceTest {

	@Autowired
	DataAccess data;
	@Autowired
	XMLSerializerService xmlService;
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
		User testUser = new User("testUser", "testPasswordHash");
		testUser.setEmail("testEmail");
		testUser.setAdmin(false);
		testUser.getNewNotifications().add(new Notification("testNotification1"));
		testUser.getOldNotifications().add(new Notification("testNotification2"));
		data.addData(testUser);
		
		//serialize and deserialize
		String filename = "user_test_file";
		List<User> users = data.getList(User.class);
		xmlService.serialize(users, filename);
		@SuppressWarnings("unchecked")
		List<User> resultUsers = (List<User>) xmlService.deserialize(filename);
		
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
		data.removeData(testUser);
	}
}
