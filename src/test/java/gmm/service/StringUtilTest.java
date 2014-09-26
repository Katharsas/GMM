package gmm.service;

import static org.junit.Assert.*;
import gmm.util.StringUtil;

import org.junit.Test;

public class StringUtilTest {
	
	@Test
	public void testContains(){
		StringUtil.IGNORE_CASE = false;
		StringUtil.ALWAYS_CONTAINS_EMPTY = true;
		assertFalse(StringUtil.contains("Bla", "bla"));
		assertTrue(StringUtil.contains("Bla", ""));
		assertTrue(StringUtil.contains("", ""));
		StringUtil.IGNORE_CASE = true;
		assertTrue(StringUtil.contains("Bla", "bla"));
		StringUtil.ALWAYS_CONTAINS_EMPTY = false;
		assertFalse(StringUtil.contains("Bla", ""));
		assertFalse(StringUtil.contains("", ""));
	}
}
