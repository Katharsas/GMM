package gmm.collections;

public class LinkedList<E> extends java.util.LinkedList<E> implements List<E>{
	private static final long serialVersionUID = -857844793375508167L;
	
	public LinkedList(java.util.Collection<? extends E> list) {
		super(list);
	}

	public LinkedList() {
		super();
	}

	@Override
	public LinkedList<E> copy() {
		return new LinkedList<E>(this);
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
}
