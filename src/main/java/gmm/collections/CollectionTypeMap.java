package gmm.collections;

import java.util.HashMap;

public class CollectionTypeMap<T> extends HashMap<Class<? extends T>, Collection<? extends T>> {

	private static final long serialVersionUID = 4834657898911264523L;
	
	/**
	 * Use {@link #putSafe(Class, Collection)} instead.
	 * @see {@link HashMap#put(Object, Object)}
	 */
	@Deprecated
	@Override
	public Collection<? extends T> put(Class<? extends T> key, Collection<? extends T> value) {
		return super.put(key, value);
	}
	
	@SuppressWarnings("unchecked")
	public <E extends T> Collection<E> putSafe(Class<E> key, Collection<E> value) {
		return (Collection<E>) super.put(key, value);
	}
	
	/**
	 * Use {@link #getSafe(Class)} instead.
	 * @see {@link HashMap#get(Object)}
	 */
	@Deprecated
	@Override
	public Collection<? extends T> get(Object key) {
		return super.get(key);
	}

	@SuppressWarnings("unchecked")
	public <E extends T> Collection<E> getSafe(Class<E> key) {
		return (Collection<E>) super.get(key);
	}
}
