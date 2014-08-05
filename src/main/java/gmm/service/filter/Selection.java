package gmm.service.filter;

/**
 * Objects of this class represent a selection (a subset) of a given set of elements of type T and
 * provide methods to manipulate this selection.
 * In particular, selections can be defined using the elements attributes to filter elements.
 * These attributes are defined and accessed via reflection using the getter method names.
 * 
 * 
 * Operations supported:
 * <ul>
 * <li>Intersect matching elements with selection</li>
 * <li>Unite matching elements with the selection</li>
 * <li>Remove matching elements from selection</li>
 * <li>Negate Selection</li>
 * </ul>
 * Example:
 * The element type is defined as:
 * <pre>
 * interface Person {
 * 	abstract String getName();
 * 	abstract Integer getAge();
 * 	abstract String getHometown();
 * 	abstract String getSex();
 * }
 * </pre>
 * 
 * You want get all persons which are "male", are 20 to 22 years old and
 * either do not have "Tom" in their name or have "Boston" being their hometown.
 * You could build an according filter as follows:
 * <pre>
 * Collection<Person> selected =
 * 	new Selection<>(allPersons, true)
 * 		.remove().matching("getName","Tom")
 * 		.uniteWith().matching("getHometown", "Boston")
 * 		.intersectWith().matching("getSex", "male")
 * 		.bufferGetter("getAge")
 * 		.intersectWith()
 * 		.matchingFilter("20")
 * 		.matchingFilter("21")
 * 		.matchingFilter("22")
 * 		.getSelected();
 * </pre>
 * 
 * @author Jan Mothes aka Kellendil
 * @param <T> - type of elements to be selected & filtered
 * @param <I> - type of data structure that gives access to the elements
 */
public interface Selection<T,I extends Iterable<T>> {
	
	/**
	 * Works similar to the method {@link #matching(String, Object)}.
	 * Matches if their type equals the given type.
	 */
	public abstract Selection<T,I> matchingType(Class<T> filter);

	/**
	 * An element matches, if a given method call on the element equals the given filter object.
	 * It also matches if the objects toString() methods are equals.
	 */
	public abstract Selection<T,I> matching(String getterMethodName, Object filter);
	
	/**
	 * Calls the method {@link #matching(String, Object)} by using a previously given filter object.
	 * This filter object must first be specified by calling the method {@link #bufferFilter(Object)}.
	 */
	public abstract Selection<T,I> matchingGetter(String getterMethodName);
	
	/**
	 * Calls the method {@link #matching(String, Object)} by using a previously given getter method name.
	 * This method name must first be specified by calling the method {@link #bufferGetter(String)}.
	 */
	public abstract Selection<T,I> matchingFilter(Object filter);
	
	/**
	 * See method filterField.
	 * @return All filtered elements. Every method adds its filtered elements to previous filtered elements.
	 */
	public abstract I getSelected();
	
	/**
	 * The filter will unite the selection with the matching elements.
	 */
	public abstract Selection<T,I> uniteWith();
	
	/**
	 * The filter will intersect the selection with the matching elements.
	 */
	public abstract Selection<T,I> intersectWith();
	
	/**
	 * The filter will remove the matching elements from the selection.
	 */
	public abstract Selection<T,I> remove();
	
	/**
	 * The filter will swap selected and unselected elements.
	 */
	public abstract Selection<T,I> negateAll();
	
	/**
	 * If true, only completely equal objects match,
	 * otherwise also partially equal objects match.
	 * Default is false.
	 */
	 public abstract void setOnlyMatchEqual(boolean onlyMatchEqual);
	 
	 /**
	  * @see {@link #matchingGetter(String)}
	  */
	 public abstract Selection<T,I> bufferFilter(Object filter);
	 
	 /**
	  * @see {@link #matchingFilter(Object)}
	  */
	 public abstract Selection<T,I> bufferGetter(String getterMethodName);
}
