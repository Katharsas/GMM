package gmm.util;

import gmm.collections.Collection;

public class Util {

	@SuppressWarnings("unchecked")
	public static <T> Class<T> classOf(T obj) {
	    return (Class<T>) obj.getClass();
	}
	
	public static <R, T> Collection<R> upCast(Collection<T> data, Class<R> to) {
		Class<T> from = data.getGenericType();
		if(to.isAssignableFrom(from)) {
			@SuppressWarnings("unchecked")
			Collection<R> result = (Collection<R>) data;
			return result;
		} else {
			throw new ClassCastException(
					"Cannot cast from Collection<"+from.getCanonicalName()
					+"> to Collection<"+to.getCanonicalName()+">!");
		}
	}
}
