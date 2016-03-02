package gmm.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

import gmm.collections.Collection;

public class Util {

	@SuppressWarnings("unchecked")
	public static <T> Class<T> classOf(T obj) {
	    return (Class<T>) obj.getClass();
	}
	
	/**
	 * Allows you to change the upper bound of a collection if the actual generic type of the
	 * collection is a subtype of the target bound.<br/>
	 * This is usefull in particular if runtime checks can garantee that the cast cannot fail by
	 * checking the collections generic type instead of iterating through every element.
	 * 
	 * Example:<pre>
	 * Colllection<?> data;
	 * Collection<? extends ChildClass> = downCastBound(data, ChildClass.class)
	 * </pre>
	 * @param <T> - upper bound target type
	 * @param data - collection with unknown generic type
	 * @param to - used to make runtime check
	 * @exception ClassCastException If the generic type of data (at runtime) is not same or
	 * subtype of target type T.
	 */
	public static <T> Collection<? extends T> castBound(Collection<?> data, Class<T> to) {
		final Class<?> from = data.getGenericType();
		if(to.isAssignableFrom(from)) {
			@SuppressWarnings("unchecked")
			final Collection<? extends T> result = (Collection<T>) data;
			return result;
		} else {
			throw new ClassCastException(
					"Cannot cast from Collection<"+from.getCanonicalName()
					+"> to Collection<? extends "+to.getCanonicalName()+">!");
		}
	}
	
	/**
	 * Allows you to change the generic type of a collection if the actual generic type of the
	 * collection is equal to the target type.
	 */
	public static <T> Collection<T> cast(Collection<?> data, Class<T> to) {
		final Class<?> from = data.getGenericType();
		if(to.equals(from)) {
			@SuppressWarnings("unchecked")
			final Collection<T> result = (Collection<T>) data;
			return result;
		} else {
			throw new ClassCastException(
					"Cannot cast from Collection<"+from.getCanonicalName()
					+"> to Collection<"+to.getCanonicalName()+">!");
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <T> Class<? extends T> getClass(T t) {
		return (Class<? extends T>) t.getClass();
	}
	
	public static class SingleIterator<T> implements Iterator<T> {
		private boolean hasNext = true;
		private T element;
		public SingleIterator(T element) {
			this.element = element;
		}
		@Override
		public boolean hasNext() {
			return hasNext;
		}
		@Override
		public T next() {
			if(hasNext) {
				hasNext = false;
				return element;
			} else throw new NoSuchElementException();
		}
	}
}
