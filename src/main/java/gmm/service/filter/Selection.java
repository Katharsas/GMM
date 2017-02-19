package gmm.service.filter;

import java.util.function.Function;

/**
 * Objects of this class represent a selection (a subset) of a given set of elements of type T and
 * provide methods to manipulate this selection.<br/>
 * In particular, selections can be defined using the elements attributes to filter elements.
 * These attributes are retrieved by calling getter functions passed with the filter elements.
 * <br/>
 * <br/>
 * Operations supported:<br/>
 * <ul>
 * <li>Intersect matching elements with selection</li>
 * <li>Unite matching elements with the selection</li>
 * <li>Remove matching elements from selection</li>
 * <li>Negate Selection</li>
 * </ul>
 * Example:<br/>
 * The element type is defined as:<br/>
 * <pre>{@code
 * interface Person {
 * 	abstract String getName();
 * 	abstract Integer getAge();
 * 	abstract String getHometown();
 * 	abstract String getSex();
 * }
 * }</pre>
 * <br/>
 * You want get all persons which are "male", are 20 to 22 years old and
 * either do not have "Tom" in their name or have "Boston" being their hometown.<br/>
 * You could build an according filter as follows:<br/>
 * <pre>{@code
 * Collection<Person> selected =
 * 	new Selection<>(allPersons, true)
 * 		.remove().matching(p -> p.getName(), "Tom")
 *		.uniteWith().matching(p -> p.getHometown(), "Boston")
 *		.strictEqual(true)
 *		.intersectWith().matching(p -> p.getSex(), "male")
 *		.cutSelected()
 *		.negateAll()
 *		.uniteWith().matchingAll(p -> p.getAge(), 20, 21, 22)
 *		.getSelected();
 * }</pre>
 * 
 * @author Jan Mothes
 * @param <T> - type of elements to be selected & filtered
 * @param <I> - type of data structure that gives access to the elements
 */
public interface Selection<T,I extends Iterable<T>> {
	
	/**
	 * Works similar to the method {@link #matching(String, Object)}.
	 * Matches if their type equals the given type.
	 */
	public abstract Selection<T,I> matchingType(Class<?> filter);

	/**
	 * An element matches, if a given method call on the element equals the given filter object.
	 * It also matches if the objects toString() methods are equals (depends on settings).
	 * @see {@link #autoConvert(boolean)}
	 */
	public abstract <F> Selection<T,I> matching(Function<T, F> getter, F filter);
	
	/**
	 * Similar to {@link #matching(String, Object)}, multiple times on the same filter object.
	 */
	@SuppressWarnings("unchecked")
	public abstract <F> Selection<T,I> matchingAll(F filter, Function<T, F>... getters);
	
	/**
	 * Similar to {@link #matching(String, Object)}, multiple times with the same method call.
	 * The method name have been first be specified by calling the method {@link #forGetter(String)}.
	 */
	@SuppressWarnings("unchecked")
	public abstract <F> Selection<T,I> matchingAll(Function<T, F> getter, F... filters);
	
	/**
	 * @return All currently selected elements.
	 */
	public abstract I getSelected();
	
	/**
	 * Removes currently unselected elements from the selection process.
	 * Any currently unselected elements will not show up anymore!
	 */
	public abstract Selection<T,I> cutSelected();
	
	/**
	 * Unites the selection with the matching elements.
	 */
	public abstract Selection<T,I> uniteWith();
	
	/**
	 * Intersects the selection with the matching elements.
	 */
	public abstract Selection<T,I> intersectWith();
	
	/**
	 * Removes the matching elements from the selection.
	 */
	public abstract Selection<T,I> remove();
	
	/**
	 * Swaps selected and unselected elements.
	 */
	public abstract Selection<T,I> negateAll();
	
	/**
	 * If true, only completely equal objects match, otherwise also partially equal objects match.
	 * Default is false.
	 */
	 public abstract Selection<T,I> strictEqual(boolean onlyMatchEqual);
	 
	 /**
	  * If true, null objects will be treated as being equal to empty strings.
	  * Default is true.
	  */
	 public abstract Selection<T,I> autoConvert(boolean nullEqualsEmptyString);
	 
}
