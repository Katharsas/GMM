package gmm.collections;

import java.util.Arrays;

public class LinkedHashSet<E> extends java.util.LinkedHashSet<E> implements Set<E> {
	private static final long serialVersionUID = 895544914638784256L;

	private final Class<E> genericType;
	
	public LinkedHashSet(Class<E> clazz, java.util.Collection<? extends E> set) {
		super(set);
		this.genericType = clazz;
	}
	
	@SafeVarargs
	public LinkedHashSet(Class<E> clazz, E... elements) {
		super(Arrays.asList(elements));
		this.genericType = clazz;
	}
	
	public LinkedHashSet(Collection<E> set) {
		super(set);
		this.genericType = set.getGenericType();
	}
	
	public LinkedHashSet(Class<E> clazz) {
		super();
		this.genericType = clazz;
	}
	
	@Override
	public LinkedHashSet<E> copy() {
		return new LinkedHashSet<E>(genericType, this);
	}
	
	@Override
	public <F> LinkedHashSet<F> newInstance(Class<F> clazz) {
		return new LinkedHashSet<F>(clazz);
	}
	
	@Override
	public Class<E> getGenericType() {
		return genericType;
	}
	
	@Override
	public String toString() {
		if (this.size() == 1) return this.iterator().next().toString();
		else {
			String result = "";	
			for (final E e : this) {
				result += "\n" + e.toString();
			}
			return result;
		}
	}
}
