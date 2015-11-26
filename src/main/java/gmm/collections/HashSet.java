package gmm.collections;

public class HashSet<E> extends java.util.HashSet<E> implements Set<E> {
	private static final long serialVersionUID = 895544914638784256L;

	private Class<E> genericType;
	
	public HashSet(Class<E> clazz, java.util.Collection<? extends E> set) {
		super(set);
		this.genericType = clazz;
	}
	
	public HashSet(Collection<E> set) {
		super(set);
		this.genericType = set.getGenericType();
	}
	
	public HashSet(Class<E> clazz) {
		super();
		this.genericType = clazz;
	}
	
	@Override
	public HashSet<E> copy() {
		return new HashSet<E>(genericType, this);
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
