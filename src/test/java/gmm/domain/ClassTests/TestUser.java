package gmm.domain.ClassTests;

import static org.junit.Assert.*;

import java.util.LinkedList;

import gmm.domain.Notification;
import gmm.domain.User;

import org.junit.Before;
import org.junit.Test;

public class TestUser {

	User objectUnderTest;
	
	@Before
	public void setUp() throws Exception {
		objectUnderTest = new User("Ralf", "123456");
	}
	
	@Test
	public void testMemberValues() {
		assertEquals("Ralf", objectUnderTest.getName());
		assertEquals("123456", objectUnderTest.getPasswordHash());
		assertEquals("", objectUnderTest.getEmail());
		assertFalse(objectUnderTest.isAdmin());
		assertEquals(new LinkedList<Notification>(), objectUnderTest.getOldNotifications());
		assertEquals(new LinkedList<Notification>(), objectUnderTest.getNewNotifications());
		
		assertFalse(objectUnderTest.sentNotificationsToMail);
		assertEquals(30, objectUnderTest.daysToSaveNotifiations);
		assertEquals(100, objectUnderTest.maximumSavedNotifications);
	}
	
	@Test
	public void testSettersAndGetters() {
		objectUnderTest.setName("Timo");
		objectUnderTest.setPasswordHash("654321");
		objectUnderTest.setEmail("Timo@project.gmm");
		objectUnderTest.setAdmin(true);
		
		assertEquals("Timo", objectUnderTest.getName());
		assertEquals("654321", objectUnderTest.getPasswordHash());
		assertEquals("Timo@project.gmm", objectUnderTest.getEmail());
		assertTrue(objectUnderTest.isAdmin());
	}
	
	@Test
	public void testNullPointerExceptions() {
		boolean thrown = true;
	    try {objectUnderTest = new User("Ralf", null);
	    	thrown = false;}
	    catch(NullPointerException e){}
	    try {objectUnderTest = new User(null, "");
	    	thrown = false;}
	    catch(NullPointerException e){}
	    try {objectUnderTest.setEmail(null);
	    	thrown = false;}
	    catch(NullPointerException e){}
	    try {objectUnderTest.setName(null);
	    	thrown = false;}
	    catch(NullPointerException e){}
	    try {objectUnderTest.setPasswordHash(null);
	    	thrown = false;}
	    catch(NullPointerException e){}
	    assertTrue(thrown);
	}
}
