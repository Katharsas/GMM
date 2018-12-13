package gmm.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedList;

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
		final LinkedList<String> results = new LinkedList<String>();
		for(int i = 0; i<10; i++) {
			final String id1 = new TestClass2().getIdLink();
			final String id2 = new TestClass3().getIdLink();
			assertFalse(id1.equals(id2));
			assertFalse(results.contains(id1)||results.contains(id2));
			Collections.addAll(results, id1, id2);
		}
	}
	
	@Test
	public void testGetFromId() {
		final int idNow = Integer.parseInt(new TestClass1().getIdLink());
		final LinkedList<TestClass1> l = new LinkedList<TestClass1>();
		for(int i = 0; i<10; i++) {
			l.add(new TestClass1("numero"+i));
		}
		for(int i = 0; i<10; i++) {
			assertEquals(UniqueObject.getFromIdLink(l, (idNow+1+i)+"").getVar(), "numero"+i);
		}
	}
	
	@Test
	public void testGetCreationDate() {
		final Instant date = Instant.now();
		final TestClass1 u = new TestClass1();
		
		final Instant uDate = u.getCreationDate();
		final long diff = uDate.toEpochMilli() - date.toEpochMilli();
		assertTrue(diff<=1000);
	}
}
