package gmm.service;

import static org.junit.Assert.*;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

public class PathTest {


	@Test
	public void test() {
		Path base = Paths.get("/folder");
		Path sub1 = Paths.get("/folder/sub");
		Path sub2 = Paths.get("/other/../folder/sub").normalize();
		Path sub3 = Paths.get("/folder/other/../sub").normalize();
		Path wrongSub = Paths.get("/folder/sub/../../other").normalize();
		
		assertTrue(sub1.startsWith(base));
		assertTrue(sub2.startsWith(base));
		assertTrue(sub3.startsWith(base));
		assertFalse(wrongSub.startsWith(base));
	}

}
