package gmm.collections;

public class UnmodifiableSet<E> extends UnmodifiableCollection<E> implements Set<E>{
	private static final long serialVersionUID = 7647640918449011742L;
	
	public UnmodifiableSet(Class<E> clazz, java.util.Set<? extends E> set) {
		super(clazz, set);
	}
	public UnmodifiableSet(Set<E> set) {
		super(set);
	}
	@Override
	public Set<E> copy() {
		return new UnmodifiableSet<E>(genericType, (Set<? extends E>)inner);
	}
}
