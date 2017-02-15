package gmm.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import gmm.util.StringUtil;

public class StringUtilTest {
	
	@Test
	public void testContains(){
		StringUtil strings = new StringUtil();
		
		assertFalse(strings.contains("Bla", "Blab"));
		
		assertFalse(strings.contains("Bla", "bla"));
		assertFalse(strings.contains("Bla", "bl"));
		
		assertTrue(strings.contains("Bla", "Bla"));
		assertTrue(strings.contains("Bla", "Bl"));
		assertTrue(strings.contains("Bla", "la"));
		
		assertTrue(strings.contains("Bla", ""));
		assertTrue(strings.contains("", ""));
		
		strings = new StringUtil(true);
		
		assertFalse(strings.contains("Bla", "Blab"));
		
		assertTrue(strings.contains("Bla", "bla"));
		assertTrue(strings.contains("Bla", "bl"));
		assertTrue(strings.contains("Bla", "la"));
		
		assertTrue(strings.contains("Bla", ""));
		assertTrue(strings.contains("", ""));
		
		strings = new StringUtil(true, false);
		
		assertFalse(strings.contains("Bla", ""));
		assertFalse(strings.contains("", ""));
	}
}
