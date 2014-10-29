package gmm.service.filter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Objects;

import gmm.util.StringUtil;

/**
 * This implementation does not alter collections passed in as arguments. The other way round,
 * changing a collection returned by a SimpleSelection object will not change the object's state.<br/>
 * <br/>
 * For this to work, a method must be passed to the constructor which makes a copy of the data
 * structure used with this class.<br/>
 * <br/>
 * TODO rename this class to "nichtzerstörende/nichtmodifizierende selection"
 * Note: Removing could have also been achieved by negating the selection and using UNION.
 * 
 * @author Jan Mothes
 */
public class CustomSelection<T,I extends Collection<T>> implements Selection<T,I> {
	
	/**
	 * Helper classes -----------------------------------------------
	 */
	
	public class Operation{
		protected CustomSelection<T,I> s;
		public Operation(CustomSelection<T,I> s) {
			this.s = s;
		}
		public Matching uniteWith() {
			return s.uniteWith()._matching;
		}
		public Matching intersectWith() {
			return s.intersectWith()._matching;
		}
		public Matching remove() {
			return s.remove()._matching;
		}
	}
	
	public class Start extends Operation implements Selection.Start {
		public Start(CustomSelection<T,I> s) {
			super(s);
		}
		public Operation strictEqual(boolean onlyMatchEqual) {
			return s.strictEqual(onlyMatchEqual)._operation;
		}
		public Operation autoConvert(boolean autoConvertToString) {
			return s.autoConvert(autoConvertToString)._operation;
		}
		public I getSelected() {
			return s.getSelected();
		}
		public Start cutSelected() {
			return s.cutSelected()._start;
		}
		public Start negateAll() {
			return s.negateAll()._start;
		}
		@Override public Selection<T,I> end() {
			return s;
		}
	}
	
	public class SecondMatching extends Start {
		public SecondMatching(CustomSelection<T,I> s) {
			super(s);
		}
		public SecondMatching matchingType(Class<T> filter) {
			return s.matchingType(filter)._secondMatching;
		}
		public SecondMatching matching(String getterMethodName, Object filter) {
			return s.matching(getterMethodName, filter)._secondMatching;
		}
	}
	
	public class Matching {
		private CustomSelection<T,I> s;
		public Matching(CustomSelection<T,I> s) {
			this.s = s;
		}
		public SecondMatching matchingType(Class<T> filter) {
			return s.matchingType(filter)._secondMatching;
		}
		public SecondMatching matching(String getterMethodName, Object filter) {
			return s.matching(getterMethodName, filter)._secondMatching;
		}
		public GetterMatching forGetter(String getterMethodName) {
			return s.forGetter(getterMethodName)._getterMatching;
		}
		public FilterMatching forFilter(Object filter) {
			return s.forFilter(filter)._filterMatching;
		}
		public I getSelected() {
			return s.getSelected();
		}
		public Selection<T,I> end() {
			return s;
		}
	}
	
	public class GetterMatching {
		private CustomSelection<T,I> s;
		public GetterMatching(CustomSelection<T,I> s) {
			this.s = s;
		}
		public Start match(Object...filters) {
			return s.matchingFilter(filters)._start;
		}
	}
	
	public class FilterMatching {
		private CustomSelection<T,I> s;
		public FilterMatching(CustomSelection<T,I> s) {
			this.s = s;
		}
		public Start match(String... getterMethodNames) {
			return s.matchingGetter(getterMethodNames)._start;
		}
	}
	
	protected Start _start = new Start(this);
	protected Operation _operation = new Operation(this);
	protected Matching _matching = new Matching(this);
	protected SecondMatching _secondMatching = new SecondMatching(this);
	protected GetterMatching _getterMatching = new GetterMatching(this);
	protected FilterMatching _filterMatching = new FilterMatching(this);
	
	
	public static interface CopyMethod<T,I> {
		public I copy(I i);
	}
	
	/**
	 * Variables -----------------------------------------------
	 */
	
	final private CopyMethod<T,I> copyMethod;
	
	private final I selected;
	private final I unselected;
	
	private String getterBuffer;
	private Object filterBuffer;
	
	private boolean AUTO_STRING_CONVERT = true;
	private boolean ONLY_MATCH_EQUAL = false;
	private boolean UNION = false;
	private boolean REMOVE = false;
	
	private final static boolean DEBUG = false;
	private final StringUtil strings = new StringUtil();
	
	/**
	 * The given elements will be the elements this selection operates on.
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
	
	@Override
	public Start start() {
		return _start;
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
	public CustomSelection<T, I> autoConvert(boolean autoConvertToString) {
		AUTO_STRING_CONVERT = autoConvertToString;
		return this;
	}
	
	/**
	 * If union, matching elements need to be found in currently unselected.
	 * If intersection, matching elements neeed to be founf in currently selected.
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
	public CustomSelection<T,I> matchingType(final Class<T> filter) {
		final I elements = this.getElements();
		for(T t : elements)	{
			final Class<? extends Object> clazz = t.getClass();
			this.applyMatching(clazz.equals(filter), t);
		}
		return this;
	}

	@Override
	public CustomSelection<T,I> matching(final String getterMethodName, final Object filter) {
		final I elements = copy(this.getElements());
		if(DEBUG) System.out.println("Matching(): "+elements.size()+" Elements");
		for (T t : elements) {
			if(DEBUG) System.out.print("Matching "+getterMethodName+" on "+t.toString()+" vs "+filter.toString());
			try {
				strings.IGNORE_CASE = true;
				strings.ALWAYS_CONTAINS_EMPTY = true;
				Method method = t.getClass().getMethod(getterMethodName);
				Object result = method.invoke(t);
				boolean matching = false;
				
				if (filter==null && result==null) {
					matching = true;
				}
				else if(filter==null || result==null) {
					matching = AUTO_STRING_CONVERT &&
							((filter==null && result.toString().equals("")) ||
							(result==null && filter.toString().equals("")));
				} else {
					if (filter instanceof String && result instanceof String) {
						matching = ONLY_MATCH_EQUAL ? 
								strings.equals((String)result,(String)filter) : 
								strings.contains((String)result,(String)filter);
					} else {
						matching = result.equals(filter) || AUTO_STRING_CONVERT && (ONLY_MATCH_EQUAL ?
								strings.equals(result.toString(), filter.toString()) :
								strings.contains(result.toString(), filter.toString()));
					}
				}
				this.applyMatching(matching, t);
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

	@Override
	public CustomSelection<T,I> matchingGetter(String... getterMethodNames) {
		Objects.requireNonNull(filterBuffer);
		for(String name : getterMethodNames) {
			matching(name, filterBuffer);
		}
		return this;
	}
	
	@Override
	public CustomSelection<T,I> matchingFilter(Object... filters) {
		Objects.requireNonNull(getterBuffer);
		for(Object filter : filters) {
			matching(getterBuffer, filter);
		}
		return this;
	}

	@Override
	public CustomSelection<T,I> forFilter(Object filter) {
		this.filterBuffer = filter;
		return this;
	}


	@Override
	public CustomSelection<T,I> forGetter(String getterMethodName) {
		this.getterBuffer = getterMethodName;
		return this;
	}

	@Override
	public CustomSelection<T,I> negateAll() {
		I buffer = copy(this.selected);
		this.selected.clear();
		this.selected.addAll(unselected);
		this.unselected.clear();
		this.unselected.addAll(buffer);
		return this;
	}
}
