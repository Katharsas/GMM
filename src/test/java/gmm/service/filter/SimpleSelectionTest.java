package gmm.service.filter;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import gmm.collections.Collection;
import gmm.collections.HashSet;
import gmm.service.filter.CustomSelection.CopyMethod;

public class SimpleSelectionTest {

	Collection<TestPerson> persons = new HashSet<>(TestPerson.class);
	Collection<TestPerson> expected = new HashSet<>(TestPerson.class);
	
	@Before
	public void before() {
		final TestPerson p01 = new TestPerson("Julian Tomston", 20, "Boston", "male");
		final TestPerson p02 = new TestPerson("Chris Tomston", 20, "New York", "male");
		final TestPerson p03 = new TestPerson("Tina Turner", 22, "Tomston", "female");
		final TestPerson p04 = new TestPerson("James Bond", 21, "NY", "male");
		final TestPerson p05 = new TestPerson("John Burner", 22, "Boston", "male");
		final TestPerson p06 = new TestPerson("King Kong", 24, "Boston", "male");
		final TestPerson p07 = new TestPerson("Tom Becks", 21, "Tomston", "male");
		final TestPerson p08 = new TestPerson("Tom Canton", 21, "Boston", "male");
		final TestPerson p09 = new TestPerson("Juliana Trevor", 20, "NY", "female");
		final TestPerson p10 = new TestPerson("Julian Bacher", 22, "Boston", "male");
		final TestPerson p11 = new TestPerson("Tommy Taster", 21, "NY", "male");
		final TestPerson p12 = new TestPerson("Julia Fletcher", 22, "NY", "female");
		final TestPerson p13 = new TestPerson("Tim Houster", 19, "Boston", "male");
		final TestPerson p14 = new TestPerson("Brian Biranha", 20, "Boston", "male");
		final TestPerson p15 = new TestPerson("Julian Bacher", 22, "NY", "male");
		
		Collections.addAll(persons, p01, p02, p03, p04, p05, p06, p07, p08, p09, p10, p11, p12, p13, p14, p15);
		Collections.addAll(expected, p01, p04, p05, p08, p10, p14, p15);
	}
	
	@After
	public void after() {
		persons.clear();
		expected.clear();
	}
	
	@Test
	public void test1_Simple() {
		final java.util.Collection<TestPerson> persons  = new java.util.HashSet<>();
		persons.addAll(this.persons);
		final java.util.Collection<TestPerson> result = new SimpleSelection<>(persons, true)
				.remove().matching(p -> p.getName(), "Tom")
				.uniteWith().matching(p -> p.getHometown(), "Boston")
				.strictEqual(true)
				.intersectWith().matching(p -> p.getSex(), "male")
				.cutSelected()
				.negateAll()
				.uniteWith().matchingAll(p -> p.getAge(), 20, 21, 22)
				.getSelected();
		
		assertEquals(expected, result);
	}
	
	@Test
	public void test2_Gmm() {
		final Collection<TestPerson> result = new GmmSelection<>(persons, true)
				.remove().matching(p -> p.getName(), "Tom")
				.uniteWith().matching(p -> p.getHometown(), "Boston")
				.strictEqual(true)
				.intersectWith().matching(p -> p.getSex(), "male")
				.cutSelected()
				.negateAll()
				.uniteWith().matchingAll(p -> p.getAge(), 20, 21, 22)
				.getSelected();
		
		assertEquals(expected, result);
	}
	
	@Test
	public void test3_Custom() {
		final Collection<TestPerson> result =
			new CustomSelection<>(persons, true, new CopyMethod<TestPerson,Collection<TestPerson>>() {
				@Override
				public Collection<TestPerson> copy(Collection<TestPerson> i) {
					return new HashSet<TestPerson>(i);
				}
			})
				.remove().matching(p -> p.getName(), "Tom")
				.uniteWith().matching(p -> p.getHometown(), "Boston")
				.strictEqual(true)
				.intersectWith().matching(p -> p.getSex(), "male")
				.cutSelected()
				.negateAll()
				.uniteWith().matchingAll(p -> p.getAge(), 20, 21, 22)
				.getSelected();
		
		assertEquals(expected, result);
	}
}
