package gmm.domain;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;

import gmm.domain.UniqueObject;

import org.junit.Before;
import org.junit.Test;

public class UniqueObjectTest {

	public class TestClass1 extends UniqueObject {
		private String var;
		public TestClass1(){
			super();}
		public TestClass1(String arg) {
			super();
			var = arg;}
		public String getVar() {
			return var;}
		@Override
		public String getIdLink() {
			return getId()+"";
		}
	}
	
	public class TestClass2 extends UniqueObject {	
	}
	public class TestClass3 extends UniqueObject {	
	}
	
	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testUniqueness() {
		LinkedList<String> results = new LinkedList<String>();
		for(int i = 0; i<10; i++) {
			String id1 = new TestClass2().getIdLink();
			String id2 = new TestClass3().getIdLink();
			assertFalse(id1.equals(id2));
			assertFalse(results.contains(id1)||results.contains(id2));
			Collections.addAll(results, id1, id2);
		}
	}
	
	@Test
	public void testGetFromId() {
		int idNow = Integer.parseInt(new TestClass1().getIdLink());
		LinkedList<TestClass1> l = new LinkedList<TestClass1>();
		for(int i = 0; i<10; i++) {
			l.add(new TestClass1("numero"+i));
		}
		for(int i = 0; i<10; i++) {
			assertEquals(UniqueObject.getFromIdLink(l, (idNow+1+i)+"").getVar(), "numero"+i);
		}
	}
	
	@Test
	public void testGetCreationDate() {
		Date date = new Date();
		TestClass1 u = new TestClass1();
		
		Date uDate = u.getCreationDate();
		long diff = uDate.getTime() - date.getTime();
		assertTrue(diff<=1000);
	}
}
