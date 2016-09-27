package gmm.collections;

public interface Iterable<E> extends java.lang.Iterable<E> {
	
	/**
	 * @return Shallow copy by using the same constructor as was used for this object.
	 */
	public Iterable<E> copy();
}
