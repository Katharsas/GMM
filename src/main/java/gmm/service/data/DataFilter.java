package gmm.service.data;

import gmm.util.List;

/**
 * Provides methods for filtering elements of type T by an attribute of these elements and stores the filtered elements.
 * Usually returns a list containing all remaining list elements. Usually does not alter input list.
 * Instantiate this class per filter operation, when multiple methods of one instance are called, the filtered elements sum up!
 * @author Jan Mothes aka Kellendil
 * @param <T> - type of list elements to be filtered
 */
public interface DataFilter<T> {
	
	/**
	 * Filters all elements whose class matches the filter.
	 * The filtered elements cane be accessed via the method getFilteredElements.
	 * @param originalList - List with the elements to be filtered, is not altered.
	 * @param filter - filter class
	 * @return List containing all elements whose attribute did not match the filter.
	 */
	public abstract List<T> filterType(List<T> originalList, Class<T> filter);
	
	/**
	 * 
	 * Filters all elements whose attribute matches the filter.
	 * A match is succesfull if the attribute equals the filter or if toString() called on the attribute contains toString() called on the filter object.
	 * The filtered elements cane be accessed via the method getFilteredElements.
	 * <pre>
	 * Example:
	 * 
	 * class Human {
	 *     private String name;
	 *     public void getName() {
	 *       return name;
	 *   }
	 * }
	 * 
	 * DataFilter<Human> filterService;
	 * List<Human> list;
	 * filterService.filterField(list, "getName", "Smith");
	 * 	//Save all "Smith" Humans in filterService.
	 * list = filterService.filterField(list, "getName", "john");
	 * 	//Add all "John" Humans. Set list to all non-"Johns".
	 * List<Human> result = filterService.getFilteredElements();
	 * 	//Retrieve all the Humans containing "John" or "Smith" in their name attribute.
	 * </pre>
	 * @param originalList - List with the elements to be filtered, is not altered.
	 * @param getterMethodName - Name of the getter method which is called on each element to get each elements attribute.
	 * @param filter - filter object
	 * @return List containing all elements whose attribute did not match the filter.
	 */
	public abstract List<T> filterField(List<T> originalList, String getterMethodName, Object filter);
	
	/**
	 * Filters out only objects which match all filter objects.
	 * See method filterField for parameters and return.
	 */
	public abstract List<T> filterAnd(List<T> originalList, String[] getterMethodNames, Object[] filters);
	
	/**
	 * Filters out all objects which match any filter objects.
	 * See method filterField for parameters and return.
	 */
	public abstract List<T> filterOr(List<T> originalList, String[] getterMethodNames, Object[] filters);

	
	/**
	 * Filters out all Tasks whose getName OR getLabel OR getAuthor OR getDetails match the filter string.
	 * @param list - List with the tasks to be filtered, is not altered.
	 * @param filter - filter String
	 * @return List containing the tasks with none of each tasks attributes matching the filter.
	 * @deprecated Use method filterOr with custom attribute getter method names instead.
	 */
	@Deprecated
	public abstract List<T> filterTask(List<T> list, String filter);
	
	/**
	 * See method filterField.
	 * @return All filtered elements. Every method adds its filtered elements to previous filtered elements.
	 */
	public abstract List<T> getFilteredElements();

	/**
	 * Clear list with filtered elements
	 */
	public abstract void clear();
	
	/**
	 * Set this filterService to filter objects/strings by equality or partial equality.
	 * False by default.
	 * @param onlyFilterEqual - True for filtering by equality, false for filtering by partial equality.
	 */
	 public abstract void setOnlyFilterEqual(boolean onlyFilterEqual);
	
}
