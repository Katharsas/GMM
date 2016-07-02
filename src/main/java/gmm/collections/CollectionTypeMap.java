package gmm.collections;

import java.util.HashMap;

public class CollectionTypeMap extends HashMap<Class<?>, Collection<?>> {

	private static final long serialVersionUID = 4834657898911264523L;
	
	@SuppressWarnings("unchecked")
	public <E> Collection<E> put(Class<E> key, Collection<E> value) {
		return (Collection<E>) super.put(key, value);
	}

	@SuppressWarnings("unchecked")
	public <E> Collection<E> get(Class<E> key) {
		return (Collection<E>) super.get(key);
	}
}
