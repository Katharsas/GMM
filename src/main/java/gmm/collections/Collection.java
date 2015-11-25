package gmm.collections;

public interface Collection<E> extends java.util.Collection<E>, Iterable<E> {
	@Override
	public Collection<E> copy();
	public Class<E> getGenericType();
}
