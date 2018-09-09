package gmm.collections;

import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Wraps a {@link Collection}. Allows attaching change listeners. List may be modified before or after
 * event listeners are called.<br>
 * 
 * <br>Does not support original addAll / removeAll  / clear operations (will iterate over elements instead).
 * <br>Does not support retainAll operation.
 * 
 * @author Jan Mothes
 */
public class EventCollection<E> implements Collection<E> {

	private final Collection<E> baseCollection;
	
	private final java.util.List<Consumer<E>> onAddListeners = new CopyOnWriteArrayList<>();
	private final java.util.List<Consumer<E>> onRemoveListeners = new CopyOnWriteArrayList<>();
	
	public EventCollection(Collection<E> baseCollection) {
		this.baseCollection = baseCollection;
	}
	
	public void registerForEventAdd(Consumer<E> callback) {
		onAddListeners.add(callback);
	}
	public void registerForEventRemove(Consumer<E> callback) {
		onRemoveListeners.add(callback);
	}

	@Override
	public int size() {
		return baseCollection.size();
	}

	@Override
	public boolean isEmpty() {
		return baseCollection.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return baseCollection.contains(o);
	}

	@Override
	public Iterator<E> iterator() {
		return baseCollection.iterator();
	}

	@Override
	public Object[] toArray() {
		return baseCollection.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return baseCollection.toArray(a);
	}

	@Override
	public boolean add(E e) {
		final boolean result = baseCollection.add(e);
		if (result) {
			for (final Consumer<E> onAdd : onAddListeners) {
				onAdd.accept(e);
			}
		}
		return result;
	}

	@Override
	public boolean remove(Object o) {
		final boolean result = baseCollection.remove(o);
		if (result) {
			@SuppressWarnings("unchecked")
			final
			E e = (E) o;
			for (final Consumer<E> onRemove : onRemoveListeners) {
				onRemove.accept(e);
			}
		}
		return result;
	}

	@Override
	public boolean containsAll(java.util.Collection<?> c) {
		return baseCollection.containsAll(c);
	}

	@Override
	public boolean addAll(java.util.Collection<? extends E> c) {
		boolean changed = false;
		for (final E  e : c) {
			changed = add(e) || changed;
		}
		return changed;
	}

	@Override
	public boolean removeAll(java.util.Collection<?> c) {
		boolean changed = false;
		for (final Object  o : c) {
			changed = remove(o) || changed;
		}
		return changed;
	}

	@Override
	public boolean retainAll(java.util.Collection<?> c) {
		throw new UnsupportedOperationException("Not implemented!");
	}

	@Override
	public void clear() {
		for (final Consumer<E> onRemove : onRemoveListeners) {
			for (final E  e : this) {
				onRemove.accept(e);
			}
		}
		baseCollection.clear();
	}

	@Override
	public Collection<E> copy() {
		return baseCollection.copy();
	}

	@Override
	public <F> Collection<F> newInstance(Class<F> clazz) {
		return baseCollection.newInstance(clazz);
	}

	@Override
	public Class<E> getGenericType() {
		return baseCollection.getGenericType();
	}
}
