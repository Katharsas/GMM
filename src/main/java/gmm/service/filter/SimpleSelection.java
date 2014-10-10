package gmm.service.filter;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.LinkedList;

/**
 * This implementation does not alter collections passed in as arguments. The other way round,
 * changing a collection returned by a SimpleSelection object will not change the object's state.<br/>
 * <br/>
 * This is achieved by using copy constructors like {@link LinkedList#LinkedList(Collection)},
 * so any passed in collections must implement a similar constructor.<br/>
 * <br/>
 * @author Jan Mothes
 */
public class SimpleSelection<T,I extends Collection<T>> extends CustomSelection<T, I> {

	private Constructor<I> constructor;
	
	public SimpleSelection(I elements, boolean selected) {
		super(elements, selected, null);
	}
	@Override
	protected I initElements(I elements) {
		if(constructor==null) {
			this.constructor = constructor(elements.getClass());
		}
		return super.initElements(elements);
	}
	
	@SuppressWarnings("unchecked")
	protected Constructor<I> constructor(Class<?> clazz) {
		final Constructor<I> cons;
		try {
			cons = (Constructor<I>) clazz.getConstructor(Collection.class);
		} catch (Exception e) {
			throw new IllegalStateException("Copy constructor not available.\n"
					+ "Please make sure your collection class has a constructor with a single"
					+ "parameter of type java.util.Collection. See cause for details.", e);
		}
		return cons;
	}
	
	@Override
	protected I copy(I i) {
		try {
			return constructor.newInstance(i);
		} catch (Exception e) {
			throw new IllegalStateException("Instanciation with copy constructor failed.\n"
					+ "Please make sure your collection class has a constructor with a single"
					+ "parameter of type java.util.Collection. See cause for details.", e);
		}
	}
}
