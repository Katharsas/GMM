package gmm.collections;

public class LinkedList<E> extends java.util.LinkedList<E> implements List<E>{
	private static final long serialVersionUID = -857844793375508167L;
	
	private Class<E> genericType;
	
	public LinkedList(Class<E> clazz, java.util.Collection<? extends E> list) {
		super(list);
		this.genericType = clazz;
	}
	
	public LinkedList(Collection<E> list) {
		super(list);
		this.genericType = (Class<E>) list.getGenericType();
	}

	public LinkedList(Class<E> clazz) {
		super();
		this.genericType = clazz;
	}

	@Override
	public LinkedList<E> copy() {
		return new LinkedList<E>(genericType, this);
	}
	
	@Override
	public String toString() {
		if (this.size() == 1) return this.iterator().next().toString();
		else {
			String result = "";	
			for (E e : this) {
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
