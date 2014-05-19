package gmm.domain.ClassTests;

import static org.junit.Assert.*;

import java.util.LinkedList;

import gmm.domain.Comment;
import gmm.domain.GeneralTask;
import gmm.domain.Priority;
import gmm.domain.Task;
import gmm.domain.TaskStatus;
import gmm.domain.User;

import org.junit.Before;
import org.junit.Test;

public class TestTask {
	
	Task objectUnderTest;
	User user;
	
	@Before
	public void setUp() throws Exception {
		user = new User("Ralf");
		objectUnderTest = new GeneralTask("testTask", user);
	}
	
	@Test
	public void testMemberValues() {
		assertEquals("testTask", objectUnderTest.getName());
		assertEquals(user, objectUnderTest.getAuthor());
		assertEquals(Priority.MID, objectUnderTest.getPriority());
		assertEquals(TaskStatus.TODO, objectUnderTest.getTaskStatus());
		assertEquals("", objectUnderTest.getDetails());
		assertEquals(new LinkedList<Comment>(), objectUnderTest.getComments());
	}
	
	@Test
	public void testSettersAndGetters() {
		objectUnderTest.setName("otherTask");
		objectUnderTest.setDetails("testDetails");
		objectUnderTest.setPriority(Priority.LOW);
		objectUnderTest.setTaskStatus(TaskStatus.DONE);
		
		assertEquals("otherTask", objectUnderTest.getName());
		assertEquals("testDetails", objectUnderTest.getDetails());
		assertEquals(Priority.LOW, objectUnderTest.getPriority());
		assertEquals(TaskStatus.DONE, objectUnderTest.getTaskStatus());
	}
	
	@Test
	public void testNullPointerExceptions() {
		boolean thrown = true;
	    try {objectUnderTest = new GeneralTask(null, user);
	    	thrown = false;}
	    catch(NullPointerException e){}
	    try {objectUnderTest = new GeneralTask("testTask", null);
	    	thrown = false;}
	    catch(NullPointerException e){}
	    try {objectUnderTest.setName(null);
	    	thrown = false;}
	    catch(NullPointerException e){}
	    try {objectUnderTest.setDetails(null);
	    	thrown = false;}
	    catch(NullPointerException e){}
	    try {objectUnderTest.setLabel(null);
	    	thrown = false;}
	    catch(NullPointerException e){}
	    try {objectUnderTest.setPriority(null);
	    	thrown = false;}
	    catch(NullPointerException e){}
	    try {objectUnderTest.setTaskStatus(null);
	    	thrown = false;}
	    catch(NullPointerException e){}
	    assertTrue(thrown);
	}
}
