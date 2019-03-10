package gmm.util;

/**
 * Can be implemented by classes that keep hold onto their generic type at runtime.
 */
public interface GenericTyped<E> {
	
	/**
	 * @return The generic type of this object.
	 */
	public Class<E> getGenericType();
}
