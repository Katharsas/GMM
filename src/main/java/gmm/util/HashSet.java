package gmm.util;

public class HashSet<E> extends java.util.HashSet<E> implements Set<E> {
	private static final long serialVersionUID = 895544914638784256L;

	public HashSet(java.util.Collection<? extends E> set) {
		super(set);
	}
	
	public HashSet() {
		super();
	}
	
	public HashSet<E> clone() {
		return new HashSet<E>(this);
	}
}
