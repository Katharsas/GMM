package gmm.collections;

public class LinkedList<E> extends java.util.LinkedList<E> implements List<E>{
	private static final long serialVersionUID = -857844793375508167L;
	
	public LinkedList(java.util.Collection<? extends E> list) {
		super(list);
	}

	public LinkedList() {
		super();
	}

	public LinkedList<E> copy() {
		return new LinkedList<E>(this);
	}
}
