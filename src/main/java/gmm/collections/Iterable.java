package gmm.collections;

public interface Iterable<E> extends java.lang.Iterable<E> {
	public <T extends Iterable<E>> T copy();
}
