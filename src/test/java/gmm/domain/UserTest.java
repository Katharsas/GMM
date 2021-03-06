package gmm.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

public class UserTest {

	User objectUnderTest;
	
	@Before
	public void setUp() throws Exception {
		objectUnderTest = new User("Ralf");
		objectUnderTest.setPasswordHash("123456");
	}
	
	@Test
	public void testMemberValues() {
		assertEquals("Ralf", objectUnderTest.getName());
		assertEquals("123456", objectUnderTest.getPasswordHash());
		assertEquals("", objectUnderTest.getEmail());
		//TODO notifications when relevant
	}
	
	@Test
	public void testSettersAndGetters() {
		objectUnderTest.setName("Timo");
		objectUnderTest.setPasswordHash("654321");
		objectUnderTest.setEmail("Timo@project.gmm");
		
		assertEquals("Timo", objectUnderTest.getName());
		assertEquals("654321", objectUnderTest.getPasswordHash());
		assertEquals("Timo@project.gmm", objectUnderTest.getEmail());
	}
	
	@Test
	public void testNullPointerExceptions() {
	    try {objectUnderTest = new User(null);fail();}
	    catch(final NullPointerException e){}
	    try {objectUnderTest.setEmail(null);fail();}
	    catch(final NullPointerException e){}
	    try {objectUnderTest.setName(null);fail();}
	    catch(final NullPointerException e){}
	    try {objectUnderTest.setPasswordHash(null);fail();}
	    catch(final NullPointerException e){}
	}
}
