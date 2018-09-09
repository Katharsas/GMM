package gmm.collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

public class EventCollectionTest {

	@Test
	public void testAdd() {
		Collection<String> c = new ArrayList<>(String.class);
		Collection<String> check = c.copy();
		
		EventCollection<String> ec = new EventCollection<>(c);
		ec.registerForEventAdd(string -> check.add(string));
		ec.add("S2");
		ec.add("S3");
		
		assertEquals(2, ec.size());
		assertEquals(c, check);
	}
	
	@Test
	public void testRemove() {
		Collection<String> c = new ArrayList<>(String.class);
		c.add("S2");
		c.add("S3");
		Collection<String> check = c.copy();
		
		EventCollection<String> ec = new EventCollection<>(c);
		ec.registerForEventRemove(string -> check.remove(string));
		ec.remove("S2");
		ec.remove("S3");
		
		assertEquals(0, ec.size());
		assertEquals(c, check);
	}
	
	@Test
	public void testAddAll() {
		Collection<String> c = new HashSet<>(String.class);
		c.add("S2");
		Collection<String> check = new ArrayList<>(String.class);
		
		EventCollection<String> ec = new EventCollection<>(c);
		ec.registerForEventAdd(string -> check.add(string));
		ec.addAll(Arrays.asList(new String[] {"S2", "S3"}));
		
		assertEquals(2, ec.size());
		assertEquals(1, check.size());
		assertTrue(c.contains("S3"));
	}
	
	@Test
	public void testRemoveAll() {
		Collection<String> c = new ArrayList<>(String.class);
		c.add("S2");
		c.add("S3");
		Collection<String> check = new ArrayList<>(String.class);
		
		EventCollection<String> ec = new EventCollection<>(c);
		ec.registerForEventRemove(string -> check.add(string));
		ec.removeAll(Arrays.asList(new String[] {"S3", "S4"}));
		
		assertEquals(1, ec.size());
		assertEquals(1, check.size());
		assertTrue(c.contains("S2"));
		assertTrue(check.contains("S3"));
	}
	
	@Test
	public void testClear() {
		Collection<String> c = new ArrayList<>(String.class);
		c.add("S2");
		Collection<String> check = new ArrayList<>(String.class);
		
		EventCollection<String> ec = new EventCollection<>(c);
		ec.registerForEventRemove(string -> check.add(string));
		ec.clear();
		
		assertEquals(0, ec.size());
		assertEquals(1, check.size());
		assertTrue(check.contains("S2"));
	}
}
