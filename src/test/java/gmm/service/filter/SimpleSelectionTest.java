package gmm.service.filter;

import static org.junit.Assert.*;

import java.util.Collections;

import gmm.collections.LinkedList;
import gmm.collections.List;

import org.junit.Test;

public class SimpleSelectionTest {

	@Test
	public void test() {
		TestPerson p01 = new TestPerson("Julian Tomston", 20, "Boston", "male");
		TestPerson p02 = new TestPerson("Chris Tomston", 20, "New York", "male");
		TestPerson p03 = new TestPerson("Tina Turner", 22, "Tomston", "female");
		TestPerson p04 = new TestPerson("James Bond", 21, "NY", "male");
		TestPerson p05 = new TestPerson("John Burner", 22, "Boston", "male");
		TestPerson p06 = new TestPerson("King Kong", 24, "Boston", "male");
		TestPerson p07 = new TestPerson("Tom Becks", 21, "Tomston", "male");
		TestPerson p08 = new TestPerson("Tom Canton", 21, "Boston", "male");
		TestPerson p09 = new TestPerson("Juliana Trevor", 20, "NY", "female");
		TestPerson p10 = new TestPerson("Julian Bacher", 22, "Boston", "male");
		TestPerson p11 = new TestPerson("Tommy Taster", 21, "NY", "male");
		TestPerson p12 = new TestPerson("Julia Fletcher", 22, "NY", "female");
		TestPerson p13= new TestPerson("Tim Houster", 19, "Boston", "male");
		TestPerson p14 = new TestPerson("Brian Biranha", 20, "Boston", "male");
		TestPerson p15 = new TestPerson("Julian Bacher", 22, "NY", "male");
		
		List<TestPerson> persons = new LinkedList<TestPerson>();
		Collections.addAll(persons, p01, p02, p03, p04, p05, p06, p07, p08, p09, p10, p11, p12, p13, p14, p15);
		
		List<TestPerson> result =
			new SimpleSelection<>(persons, true)
				.remove().matching("getName","Tom")
				.uniteWith().matching("getHometown", "Boston")
				.intersectWith().matching("getSex", "male")
				.bufferGetter("getAge")
				.intersectWith()
				.matchingFilter("20")
				.matchingFilter("21")
				.matchingFilter("22")
				.getSelected();
		
		List<TestPerson> expected = new LinkedList<TestPerson>();
		Collections.addAll(persons, p01, p04, p05, p08, p10, p14, p15);
		
		assertEquals(expected, result);
	}

}
