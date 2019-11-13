package gmm.collections;

import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

public class UnmodifiableCollection<E> implements Collection<E>, java.io.Serializable {
	private static final long serialVersionUID = -1502692809877910031L;
	
	protected final java.util.Collection<E> inner;
	@XStreamAsAttribute
	protected final Class<E> genericType;
	@XStreamAsAttribute
	private final boolean hasNewInstanceMethod;
	
	@SuppressWarnings("unchecked")
	public UnmodifiableCollection(Class<E> clazz, java.util.Collection<? extends E> collection) {
		Objects.requireNonNull(clazz);
		Objects.requireNonNull(collection);
		this.genericType = clazz;
		inner = (java.util.Collection<E>) collection;
		hasNewInstanceMethod = false;
	}
	
	public UnmodifiableCollection(Collection<E> collection) {
		Objects.requireNonNull(collection);
		this.genericType = collection.getGenericType();
		inner = collection;
		hasNewInstanceMethod = true;
	}
	
	@Override public int size() {
		return inner.size();
	}
	@Override public boolean isEmpty() {
		return inner.isEmpty();
	}
	@Override public boolean contains(Object o) {
		return inner.contains(o);
	}
	@Override public Object[] toArray() {
		return inner.toArray();
	}
	@Override public <T> T[] toArray(T[] a) {
		return inner.toArray(a);
	}
	@Override public String toString() {
		return inner.toString();
	}
	
	@Override
	public Iterator<E> iterator() {
        return new Iterator<E>() {
            private final Iterator<? extends E> it = inner.iterator();
            @Override public boolean hasNext() {
            	return it.hasNext();
            }
            @Override public E next() {
            	return it.next();
            }
            @Override public void remove() {
                throw new UnsupportedOperationException();
            }
            @Override
            public void forEachRemaining(Consumer<? super E> action) {
                it.forEachRemaining(action);
            }
        };
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
		return inner.containsAll(c);
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
    public void forEach(Consumer<? super E> action) {
        inner.forEach(action);
    }
    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        throw new UnsupportedOperationException();
    }
    @Override
    public Spliterator<E> spliterator() {
        return (Spliterator<E>)inner.spliterator();
    }
    @Override
    public Stream<E> stream() {
        return (Stream<E>)inner.stream();
    }
    @Override
    public Stream<E> parallelStream() {
        return (Stream<E>)inner.parallelStream();
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
