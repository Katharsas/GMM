package gmm.service.filter;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;

import gmm.util.StringUtil;

/**
 * This implementation does not alter collections passed in as arguments. The other way round,
 * changing a collection returned by a SimpleSelection object will not change the object's state.<br>
 * <br>
 * For this to work, a method must be passed to the constructor which makes a copy of the data
 * structure used with this class.<br>
 * <br>
 * Note: Removing could have also been achieved by negating the selection and using UNION.
 * 
 * @author Jan Mothes
 */
public class CustomSelection<T,I extends Collection<T>> implements Selection<T,I> {
	
	public static interface CopyMethod<T,I> {
		public I copy(I i);
	}
	
	/**
	 * Variables -----------------------------------------------
	 */
	
	final private CopyMethod<T,I> copyMethod;
	
	private final I selected;
	private final I unselected;
	
	private boolean NULL_EQUALS_EMPTY = true;
	private boolean ONLY_MATCH_EQUAL = false;
	private boolean UNION = false;
	private boolean REMOVE = false;
	
	private final static boolean DEBUG = false;
	private final StringUtil strings = StringUtil.ignoreCase();
	
	/**
	 * The given elements will be the elements this selection operates on. The collection itself
	 * will not be modified, instead a copyMethod will be used to create working copies.
	 * @param elements - The elements.
	 * @param selected - If true, the elements are going to be selected, otherwise unselected.
	 */
	public CustomSelection(final I elements, final boolean selected, CopyMethod<T,I> copyMethod) {
		this.copyMethod = copyMethod;
		this.selected = copy(initElements(elements));
		this.unselected = copy(initElements(elements));
		if(selected) {
			this.unselected.clear();
		}
		else {
			this.selected.clear();
		}
	}
	
	protected I initElements(I elements) {
		return elements;
	}
	
	protected I copy(I i) {
		return copyMethod.copy(i);
	}
	
	@Override
	public CustomSelection<T,I> strictEqual(boolean onlyMatchEqual){
		ONLY_MATCH_EQUAL = onlyMatchEqual;
		return this;
	}
	
	@Override
	public CustomSelection<T, I> autoConvert(boolean nullEqualsEmptyString) {
		NULL_EQUALS_EMPTY = nullEqualsEmptyString;
		return this;
	}
	
	/**
	 * If union, matching elements need to be found in currently unselected.
	 * If intersection, matching elements neeed to be found in currently selected.
	 * 
	 * @return The elements on which the matching needs to be applied.
	 */
	protected I getElements() {
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
	public CustomSelection<T,I> matchingType(final Class<?> filter) {
		final I elements = copy(this.getElements());
		for(final T t : elements)	{
			final Class<?> clazz = t.getClass();
			this.applyMatching(filter.isAssignableFrom(clazz), t);
		}
		return this;
	}
	
	@Override
	public <F> CustomSelection<T,I> matching(Function<T, F> getter, F filter) {
		final I elements = copy(this.getElements());
		if(DEBUG) System.out.println("Matching(): "+elements.size()+" Elements");
		for (final T t : elements) {
			if(DEBUG) System.out.print("Matching function applied on "+t.toString()+" vs "+filter.toString());
			
			final F result = getter.apply(t);
			
			boolean matching = false;
			if (filter==null && result==null) {
				matching = true;
			}
			else if(result instanceof String && filter instanceof String) {
				matching = doStringsMatch((String) result, (String) filter);
			} else {
				if(filter==null || result==null) {
					matching = false;
				} else {
					matching = result.equals(filter);
				}
			}
			this.applyMatching(matching, t);
		}
		return this;
	}
	
	private boolean doStringsMatch(String result, String filter) {
		if(filter==null || result==null) {
			return NULL_EQUALS_EMPTY &&
					((filter==null && result.equals("")) ||
					(result==null && filter.equals("")));
		} else {
			return ONLY_MATCH_EQUAL ? 
					strings.equals(result,filter) : 
					strings.contains(result,filter);
		}
	}

	@Override
	public I getSelected() {
		return copy(selected);
	}
	
	@Override
	public CustomSelection<T, I> cutSelected() {
		unselected.clear();
		return this;
	}

	@Override
	public CustomSelection<T,I> uniteWith() {
		this.REMOVE = false;
		this.UNION = true;
		return this;
	}

	@Override
	public CustomSelection<T,I> intersectWith() {
		this.REMOVE = false;
		this.UNION = false;
		return this;
	}
	
	@Override
	public CustomSelection<T,I> remove() {
		this.REMOVE = true;
		return this;
	}
	
	@SafeVarargs
	@Override
	public final <F> Selection<T, I> matchingAll(F filter, Function<T, F>... getters) {
		Objects.requireNonNull(filter);
		for(final Function<T, F> getter : getters) {
			matching(getter, filter);
		}
		return this;
	}

	@SafeVarargs
	@Override
	public final <F> Selection<T, I> matchingAll(Function<T, F> getter, F... filters) {
		Objects.requireNonNull(getter);
		for(final F filter : filters) {
			matching(getter, filter);
		}
		return this;
	}

	@Override
	public CustomSelection<T,I> negateAll() {
		final I buffer = copy(this.selected);
		this.selected.clear();
		this.selected.addAll(unselected);
		this.unselected.clear();
		this.unselected.addAll(buffer);
		return this;
	}
}
