package gmm.collections;

/**
 * Extends {@link java.util.Collection} by knowing its generic type at runtime.
 * <br>
 * <br>Implementations:<ul>
 * <li>{@link ArrayList}</li>
 * <li>{@link LinkedList}</li>
 * <li>{@link HashSet}</li>
 * <li>{@link LinkedHashSet}</li>
 * </ul>
 * <br>Decorators / Views:<ul>
 * <li>{@link JoinedCollectionView}</li>
 * <li>{@link UnmodifiableCollection}</li>
 * <li>{@link EventCollection}</li>
 * </ul>
 * @author Jan Mothes
 */
public interface Collection<E> extends java.util.Collection<E>, Iterable<E> {
	
	/**
	 * @return Shallow copy with the same collection type as this objects real collection type.
	 */
	@Override
	public Collection<E> copy();
	
	/**
	 * @return New empty instance of the same type as this objects real collection type.
	 */
	public <F> Collection<F> newInstance(Class<F> clazz);
	
	/**
	 * @return The generic type that was used to instantiate this collection object.
	 */
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
