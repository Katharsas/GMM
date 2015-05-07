package gmm.domain;

import static org.junit.Assert.*;

import java.util.LinkedList;

import gmm.domain.Comment;
import gmm.domain.User;
import gmm.domain.task.GeneralTask;
import gmm.domain.task.Task;
import gmm.domain.task.TaskPriority;
import gmm.domain.task.TaskStatus;

import org.junit.Before;
import org.junit.Test;

public class TaskTest {
	
	Task objectUnderTest;
	User user;
	
	@Before
	public void setUp() throws Exception {
		user = new User("Ralf");
		objectUnderTest = new GeneralTask(user);
	}
	
	@Test
	public void testMemberValues() {
		assertEquals("", objectUnderTest.getName());
		assertEquals(user, objectUnderTest.getAuthor());
		assertEquals(TaskPriority.MID, objectUnderTest.getPriority());
		assertEquals(TaskStatus.TODO, objectUnderTest.getTaskStatus());
		assertEquals("", objectUnderTest.getDetails());
		assertEquals(new LinkedList<Comment>(), objectUnderTest.getComments());
	}
	
	@Test
	public void testSettersAndGetters() {
		objectUnderTest.setName("otherTask");
		objectUnderTest.setDetails("testDetails");
		objectUnderTest.setPriority(TaskPriority.LOW);
		objectUnderTest.setTaskStatus(TaskStatus.DONE);
		
		assertEquals("otherTask", objectUnderTest.getName());
		assertEquals("testDetails", objectUnderTest.getDetails());
		assertEquals(TaskPriority.LOW, objectUnderTest.getPriority());
		assertEquals(TaskStatus.DONE, objectUnderTest.getTaskStatus());
	}
	
	@Test
	public void testNullPointerExceptions() throws Exception {
		boolean thrown = true;
	    try {objectUnderTest = new GeneralTask(null);
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
