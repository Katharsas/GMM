package gmm.service;

import static org.junit.Assert.*;
import gmm.util.StringUtil;

import org.junit.Test;

public class StringUtilTest {
	
	@Test
	public void testContains(){
		StringUtil strings = new StringUtil();
		strings.IGNORE_CASE = false;
		strings.ALWAYS_CONTAINS_EMPTY = true;
		assertFalse(strings.contains("Bla", "bla"));
		assertTrue(strings.contains("Bla", ""));
		assertTrue(strings.contains("", ""));
		strings.IGNORE_CASE = true;
		assertTrue(strings.contains("Bla", "bla"));
		strings.ALWAYS_CONTAINS_EMPTY = false;
		assertFalse(strings.contains("Bla", ""));
		assertFalse(strings.contains("", ""));
	}
}
