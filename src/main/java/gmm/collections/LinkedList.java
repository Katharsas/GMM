package gmm.collections;

import java.util.Arrays;
import java.util.HashMap;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

public class LinkedList<E> extends java.util.LinkedList<E> implements List<E>{
	private static final long serialVersionUID = -857844793375508167L;
	
	public static <K,V> Multimap<K,V> getMultiMap(Class<V> clazz) {
		return Multimaps.newListMultimap(
				new HashMap<K, java.util.Collection<V>>(), () -> new LinkedList<V>(clazz));
	}
	
	private final Class<E> genericType;
	
	public LinkedList(Class<E> clazz) {
		super();
		this.genericType = clazz;
	}
	
	@SafeVarargs
	public LinkedList(Class<E> clazz, E... elements) {
		super(Arrays.asList(elements));
		this.genericType = clazz;
	}
	
	public LinkedList(Class<E> clazz, java.util.Collection<? extends E> list) {
		super(list);
		this.genericType = clazz;
	}
	
	public LinkedList(Collection<E> list) {
		super(list);
		this.genericType = list.getGenericType();
	}

	@Override
	public LinkedList<E> copy() {
		return new LinkedList<E>(genericType, this);
	}
	
	@Override
	public <F> LinkedList<F> newInstance(Class<F> clazz) {
		return new LinkedList<F>(clazz);
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

	@Override
	public Class<E> getGenericType() {
		return genericType;
	}
}
