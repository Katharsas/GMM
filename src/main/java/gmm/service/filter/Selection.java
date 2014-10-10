package gmm.service.filter;

/**
 * Objects of this class represent a selection (a subset) of a given set of elements of type T and
 * provide methods to manipulate this selection.<br/>
 * In particular, selections can be defined using the elements attributes to filter elements.
 * These attributes are defined and accessed via reflection using the getter method names.
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
 * <pre>
 * interface Person {
 * 	abstract String getName();
 * 	abstract Integer getAge();
 * 	abstract String getHometown();
 * 	abstract String getSex();
 * }
 * </pre>
 * <br/>
 * You want get all persons which are "male", are 20 to 22 years old and
 * either do not have "Tom" in their name or have "Boston" being their hometown.<br/>
 * You could build an according filter as follows:<br/>
 * <pre>
 * Collection<Person> selected =
 * 	new Selection<>(allPersons, true).start()
 * 		.remove().matching("getName","Tom")
 * 		.uniteWith().matching("getHometown", "Boston")
 * 		.strictEqual(true)
 * 		.intersectWith().matching("getSex", "male")
 * 		.cutSelected()
 * 		.negateAll()
 * 		.uniteWith().forGetter("getAge").match(20, 21, "22")
 * 		.getSelected();
 * </pre>
 * 
 * @author Jan Mothes aka Kellendil
 * @param <T> - type of elements to be selected & filtered
 * @param <I> - type of data structure that gives access to the elements
 */
public interface Selection<T,I extends Iterable<T>> {
	
	public interface Start {
		public Selection<?, ?> end();
	}
	
	/**
	 * Returns a helper object which only offers a reasonable choice of actions.
	 * All actions will return helper objects too which also only offer limited actions.
	 * Use this if you are not very familiar with this tool.<br/>
	 * <br/>
	 * Use the {@link Start#end()} method to quit the helper object chain and return
	 * to the main object wich does not restrict access to any function. (Note that you cannot
	 * quit anywhere in the chain).
	 */
	public Start start();
	
	/**
	 * Works similar to the method {@link #matching(String, Object)}.
	 * Matches if their type equals the given type.
	 */
	public abstract Selection<T,I> matchingType(Class<T> filter);

	/**
	 * An element matches, if a given method call on the element equals the given filter object.
	 * It also matches if the objects toString() methods are equals (depends on settings).
	 * @see {@link #autoConvert(boolean)}
	 */
	public abstract Selection<T,I> matching(String getterMethodName, Object filter);
	
	/**
	 * Calls the method {@link #matching(String, Object)} while using a previously given filter object.
	 * This filter object must have been specified by calling the method {@link #forFilter(Object)}.
	 */
	public abstract Selection<T,I> matchingGetter(String...getterMethodNames);
	
	/**
	 * Calls the method {@link #matching(String, Object)} while using a previously given getter method name.
	 * The method name have been first be specified by calling the method {@link #forGetter(String)}.
	 */
	public abstract Selection<T,I> matchingFilter(Object...filters);
	
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
	 * If true, only completely equal objects match,
	 * otherwise also partially equal objects match.
	 * Default is false.
	 */
	 public abstract Selection<T,I> strictEqual(boolean onlyMatchEqual);
	 
	 /**
	  * If true, objects will automatically be converted to Strings to find
	  * a match, otherwise objects must always be of the same type to match.
	  * Default is true.
	  */
	 public abstract Selection<T,I> autoConvert(boolean autoConvertToString);
	 
	 /**
	  * @see {@link Selection#matchingGetter(String)}
	  */
	 public abstract Selection<T,I> forFilter(Object filter);
	 
	 /**
	  * @see {@link Selection#matchingFilter(Object)}
	  */
	 public abstract Selection<T,I> forGetter(String getterMethodName);
}
