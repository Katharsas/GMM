package gmm.collections;

import java.util.Iterator;

public class UnmodifiableCollection<E> implements Collection<E>, java.io.Serializable {
	private static final long serialVersionUID = -1502692809877910031L;
	
	private final java.util.Collection<E> inner;
	private final Class<E> genericType;
	private final boolean hasNewInstanceMethod;
	
	@SuppressWarnings("unchecked")
	public UnmodifiableCollection(Class<E> clazz, java.util.Collection<? extends E> collection) {
		this.genericType = clazz;
		inner = (java.util.Collection<E>) collection;
		hasNewInstanceMethod = false;
	}
	
	public UnmodifiableCollection(Collection<E> collection) {
		this.genericType = collection.getGenericType();
		inner = collection;
		hasNewInstanceMethod = true;
	}
	
	@Override
	public int size() {
		return inner.size();
	}

	@Override
	public boolean isEmpty() {
		return inner.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return inner.contains(o);
	}

	@Override
	public Iterator<E> iterator() {
		return inner.iterator();
	}

	@Override
	public Object[] toArray() {
		return inner.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return inner.toArray(a);
	}

	@Override
	public boolean add(E e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsAll(java.util.Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(java.util.Collection<? extends E> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(java.util.Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(java.util.Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Collection<E> copy() {
		return new UnmodifiableCollection<>(genericType, inner);
	}

	@Override
	public <F> Collection<F> newInstance(Class<F> clazz) {
		if (hasNewInstanceMethod) {
			return ((Collection<E>) inner).newInstance(clazz);
		} else {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public Class<E> getGenericType() {
		return genericType;
	}

}
