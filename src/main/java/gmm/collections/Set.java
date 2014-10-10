package gmm.collections;


public interface Set<E> extends java.util.Set<E>, Collection<E> {
	@Override
	public Set<E> copy();
}
