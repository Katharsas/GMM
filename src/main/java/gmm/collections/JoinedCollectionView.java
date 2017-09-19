package gmm.collections;

import java.util.Arrays;
import java.util.Iterator;

import com.google.common.collect.Iterables;

public class JoinedCollectionView<E> implements Collection<E>, java.io.Serializable {
	private static final long serialVersionUID = -5898627844171252241L;
	
	private final Collection<? extends E>[] items;
	private final Collection<E> copyTarget;
	private final Class<E> genericType;
	
	/**
	 * Returns a live aggregated collection view of the collections passed in.
	 * All modifying methods except clear throw {@link UnsupportedOperationException}.
	 * <br>
	 * None of the above methods is thread safe (nor would there be an easy way
	 * of making them).
	 * 
	 * @param copyTarget - Collection used to merge item collections into when a non-read-only,
	 * 		non-live copy of the collections of this view is needed by {@link #copy()} method. Will
	 * 		be cleared before used.
	 */
	@SafeVarargs
	public JoinedCollectionView(Collection<E> copyTarget, Collection<? extends E>... items) {
		this.items = items;
		this.copyTarget = copyTarget;
		this.copyTarget.clear();
		genericType = copyTarget.getGenericType();
	}

	@Override
	public boolean add(E e) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean addAll(java.util.Collection<? extends E> c) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean retainAll(final java.util.Collection<?> c) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean remove(final Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(final java.util.Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		for (final Collection<? extends E> coll : items) {
            coll.clear();
        }
	}

	@Override
	public boolean contains(Object o) {
		for (final Collection<? extends E> coll : items) {
			if (coll.contains(o)) return true;
		}
		return false;
	}

	@Override
	public boolean containsAll(java.util.Collection<?> c) {
		for (final Object o : c) {
			if (!contains(o)) return false;
		}
		return true;
	}

	@Override
	public boolean isEmpty() {
		for (final Collection<? extends E> coll : items) {
			if (!coll.isEmpty()) return false;
		}
		return true;
	}

	@Override
	public Iterator<E> iterator() {
		return Iterables.concat(items).iterator();
	}

	@Override
	public int size() {
		int ct = 0;
		for (final Collection<? extends E> coll : items) {
			ct += coll.size();
		}
		return ct;
	}

	@Override
	public Object[] toArray() {
		int length = 0;
		for (final Collection<? extends E> coll : items) {
			length += coll.size();
		}
		final Object[] array = new Object[length];
		final Iterator<E> it = iterator();
		for(int i = 0; i < length; i++) {
			array[i] = it.next();
		}
		return array;
	}

	@Override
	public <T> T[] toArray(T[] a) {
		int length = 0;
		for (final Collection<? extends E> coll : items) {
			length += coll.size();
		}
		if (a.length < length) {
			@SuppressWarnings("unchecked")
			final
			T[] result = (T[]) toArray();
			return result;
		} else {
			final Iterator<E> it = iterator();
			for(int i = 0; i < a.length; i++) {
				if (it.hasNext()) {
					@SuppressWarnings("unchecked")
					final
					T next = (T) it.next();
					a[i] = next;
				} else {
					a[i] = null;
				}
			}
			return a;
		}
	}

	/**
	 * Since copying a read-only-view just to get the same read-only-view does not make much sense,
	 * this method actually returns a proper collection as the caller would probably expect it to
	 * do, since he probably does not now that this is just a view. <br>
	 * To make an copy of the actual view itself, use {@link #viewCopy()}.
	 */
	@Override
	public Collection<E> copy() {
		final Collection<E> freshTarget = copyTarget.copy();
		for (final Collection<? extends E> coll : items) {
			freshTarget.addAll(coll);
		}
		return freshTarget;
	}
	
	public JoinedCollectionView<E> viewCopy() {
		final Collection<? extends E>[] newItems = Arrays.copyOf(items, items.length);
		for(int i = 0; i < items.length; i++) {
			newItems[i] = items[i].copy();
		}
		return new JoinedCollectionView<>(copyTarget, newItems);
	}
	
	/**
	 * Since returning an empty read-only-view that cannot be populated with elements does not make
	 * much sense, this method actually returns a new instance of a proper collection as the caller
	 * would probably expect it to do, if he does not know this is a read-only-view (if he knew he
	 * could just use the constructor).
	 */
	@Override
	public <F> Collection<F> newInstance(Class<F> clazz) {
		return copyTarget.newInstance(clazz);
	}

	@Override
	public Class<E> getGenericType() {
		return genericType;
	}
}
