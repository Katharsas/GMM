package gmm.domain;

import static org.junit.Assert.assertEquals;

import gmm.domain.task.GeneralTask;
import gmm.domain.task.Task;
import gmm.util.Util;

/**
 * Deep equals for domain class objects.
 * 
 * @author Jan Mothes
 */
public class AssertEquals {

	private static void assertEqualsUnique(UniqueObject expected, UniqueObject result) {
		assertEquals(expected.getIdLink(), result.getIdLink());
		assertEquals(expected.getId(), expected.getId());
	}
	
	private static void assertEqualsNamed(NamedObject expected, NamedObject result) {
		assertEqualsUnique(expected, result);
		assertEquals(expected.getName(), result.getName());
	}
	
	public static void assertEqualsUser(User expected, User result) {
		assertEqualsNamed(expected, result);
		assertEquals(expected.getCreationDate(), result.getCreationDate());
		assertEquals(expected.getEmail(), result.getEmail());
		assertEquals(expected.getPasswordHash(), result.getPasswordHash());
		// TODO Notifications, when they become relevant
	}
	
	private static void assertEqualsComment(Comment expected, Comment result) {
		assertEqualsUnique(expected, result);
		assertEquals(expected.getText(), result.getText());
		assertEqualsUser(expected.getAuthor(), result.getAuthor());
		assertEquals(expected.getCreationDate(), result.getCreationDate());
		assertEquals(expected.getLastEditedDate(), result.getLastEditedDate());
	}
	
	private static void assertEqualsTask(Task expected, Task result) {
		assertEqualsNamed(expected, result);
		assertEquals(expected.getAuthor(), result.getAuthor());
		assertEquals(expected.getAssigned(), result.getAssigned());
		assertEquals(expected.getLabel(), result.getLabel());
		assertEquals(expected.getDetails(), result.getDetails());
		assertEquals(expected.getCreationDate(), result.getCreationDate());
		assertEquals(expected.getTaskStatus(), result.getTaskStatus());
		assertEquals(expected.getPriority(), result.getPriority());
		Util.zip(expected.getComments(), result.getComments(), (expectedComment, resultComment)
				-> assertEqualsComment(expectedComment, resultComment));
		// TODO Task Dependencies, when they become relevant
	}
	
	public static void assertEqualsGeneralTask(GeneralTask expected, GeneralTask result) {
		assertEqualsTask(expected, result);
	}
}
