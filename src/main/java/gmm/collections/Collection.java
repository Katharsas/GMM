package gmm.collections;

public interface Collection<E> extends java.util.Collection<E>, Iterable<E> {
	@Override
	public Collection<E> copy();
	public Class<E> getGenericType();
	
	@SuppressWarnings("unchecked")
	public static <E> Class<Collection<E>> getClassGeneric(Class<E> clazz) {
		return (Class<Collection<E>>) (Class<?>) Collection.class;
	}
	
	@SuppressWarnings("unchecked")
	public static Class<Collection<?>> getClassAny() {
		return (Class<Collection<?>>) (Class<?>) Collection.class;
	}
}
