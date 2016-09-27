package gmm.collections;

public interface List<E> extends java.util.List<E>, Collection<E>{
	@Override
	public List<E> copy();
	@Override
	public <F> List<F> newInstance(Class<F> clazz);
}
