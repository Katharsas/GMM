package gmm.service.filter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

import gmm.util.Collection;
import gmm.util.StringUtil;

/**
 * Removing could have also been achieved by negating the selection and using UNION.
 * @author Jan
 *
 * @param <T> The type of the elements that will be selected/filtered.
 */
public class SimpleSelection<T> implements Selection<T> {
	
	private final Collection<T> selected;
	private final Collection<T> unselected;
	
	private String getterBuffer;
	private Object filterBuffer;
	
	private boolean ONLY_MATCH_EQUAL = false;
	private boolean UNION = false;
	private boolean REMOVE = false;
	
	private final static boolean DEBUG = false;
	
	/**
	 * The given elements will be the elements this selection operates on.
	 * @param elements - The elements.
	 * @param selected - If true, the elements are going to be selected, otherwise unselected.
	 */
	public SimpleSelection(final Collection<T> elements, final boolean selected) {
		this.selected = elements.clone();
		this.unselected = elements.clone();
		if(selected) {
			this.unselected.clear();
		}
		else {
			this.selected.clear();
		}
	}
	
	@Override
	public void setOnlyMatchEqual(boolean onlyMatchEqual){
		ONLY_MATCH_EQUAL = onlyMatchEqual;
	}
	
	/**
	 * If union, matching elements need to be found in currently unselected.
	 * If intersection, matching elements neeed to be founf in currently selected.
	 * 
	 * @return The elements on which the matching needs to be applied.
	 */
	protected Collection<T> getElements() {
		return !UNION || REMOVE ? selected : unselected;
	}
	
	/**
	 * If union, add the matching elements to selected elements.
	 * If intersection, remove the NOT matching elements from selected elements.
	 * if remove, remove the matching elements from selected elements.
	 * 
	 * @param isMatching - True if the elements is matches the filter, false otherwise.
	 * @param t - The element.
	 */
	protected void applyMatching(final boolean isMatching, final T t) {
		if(DEBUG) System.out.println(" ("+isMatching+")");
		if(isMatching) {
			if(REMOVE) {
				selected.remove(t);
				unselected.add(t);
				return;
			}
			if(UNION) {
				selected.add(t);
				unselected.remove(t);
				return;
			}
		}
		else {
			if(REMOVE) {
				return;
			}
			if(!UNION) {
				selected.remove(t);
				unselected.add(t);
				return;
			}
		}
	}
	
	@Override
	public Selection<T> matchingType(final Class<T> filter) {
		final Collection<T> elements = this.getElements().clone();
		for(T t : elements)	{
			final Class<? extends Object> clazz = t.getClass();
			this.applyMatching(clazz.equals(filter), t);
		}
		return this;
	}

	@Override
	public Selection<T> matching(final String getterMethodName, final Object filter) {
		final Collection<T> elements = this.getElements().clone();
		if(DEBUG) System.out.println("Matching(): "+elements.size()+" Elements");
		for (T t : elements) {
			if(DEBUG) System.out.print("Matching "+getterMethodName+" on "+t.toString()+" vs "+filter.toString());
			try {
				StringUtil.IGNORE_CASE = true;
				StringUtil.ALWAYS_CONTAINS_EMPTY = true;
				Method method = t.getClass().getMethod(getterMethodName);
				Object result = method.invoke(t);
				
				boolean bothNull = filter==null && result==null;
				boolean isNull = filter==null || result==null;
				boolean bothEmpty = isNull &&
						(filter==null && result.toString().equals("")) ||
						(result==null && filter.toString().equals(""));
				
				if (bothNull || bothEmpty) {
					this.applyMatching(true, t);
					return this;
				}
				
				boolean string = filter instanceof String && result instanceof String;
				boolean contains = string && (ONLY_MATCH_EQUAL ? 
						StringUtil.equals((String)result,(String)filter) : 
						StringUtil.contains((String)result,(String)filter));
				boolean equalsOrContains = !isNull && (contains || result.equals(filter)); 
				boolean equalsOrContainsOrToString = !isNull && (equalsOrContains || (ONLY_MATCH_EQUAL ?
						StringUtil.equals(result.toString(), filter.toString()) :
						StringUtil.contains(result.toString(), filter.toString())));
				
				this.applyMatching(bothNull || equalsOrContainsOrToString, t);
			}
			catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				System.err.println("SimpleSelection Error: Wrong getterMethodName or wrong list item!");
				System.err.println("GetterMethod must be implemented by all list items.");
				e.printStackTrace();
			}
		}
		return this;
	}

	@Override
	public Collection<T> getSelected() {
		return selected.clone();
	}

	@Override
	public Selection<T> uniteWith() {
		this.REMOVE = false;
		this.UNION = true;
		return this;
	}

	@Override
	public Selection<T> intersectWith() {
		this.REMOVE = false;
		this.UNION = false;
		return this;
	}
	
	@Override
	public Selection<T> remove() {
		this.REMOVE = true;
		return this;
	}

	@Override
	public Selection<T> matchingGetter(String getterMethodName) {
		Objects.requireNonNull(filterBuffer);
		return matching(getterMethodName, filterBuffer);
	}
	
	@Override
	public Selection<T> matchingFilter(Object filter) {
		Objects.requireNonNull(getterBuffer);
		return matching(getterBuffer, filter);
	}

	@Override
	public Selection<T> bufferFilter(Object filter) {
		this.filterBuffer = filter;
		return this;
	}


	@Override
	public Selection<T> bufferGetter(String getterMethodName) {
		this.getterBuffer = getterMethodName;
		return this;
	}

	@Override
	public Selection<T> negateAll() {
		Collection<T> buffer = this.selected;
		this.selected.clear();
		this.selected.addAll(unselected);
		this.unselected.clear();
		this.unselected.addAll(buffer);
		return this;
	}
}
