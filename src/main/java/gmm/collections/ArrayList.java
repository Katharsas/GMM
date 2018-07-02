package gmm.collections;

import java.util.Arrays;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

public class ArrayList<E> extends java.util.ArrayList<E> implements List<E> {
	private static final long serialVersionUID = -857844793375508167L;
	
	@XStreamAsAttribute
	private final Class<E> genericType;
	
	public ArrayList(Class<E> clazz) {
		super();
		this.genericType = clazz;
	}
	
	@SafeVarargs
	public ArrayList(Class<E> clazz, E... elements) {
		super(Arrays.asList(elements));
		this.genericType = clazz;
	}
	
	public ArrayList(Class<E> clazz, java.util.Collection<? extends E> list) {
		super(list);
		this.genericType = clazz;
	}
	
	public ArrayList(Class<E> clazz, int initialCapacity) {
		super(initialCapacity);
		this.genericType = clazz;
	}
	
	public ArrayList(Collection<E> list) {
		super(list);
		this.genericType = list.getGenericType();
	}

	@Override
	public ArrayList<E> copy() {
		return new ArrayList<E>(genericType, this);
	}
	
	@Override
	public <F> ArrayList<F> newInstance(Class<F> clazz) {
		return new ArrayList<F>(clazz);
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
