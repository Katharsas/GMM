package gmm.util;

import java.util.Enumeration;
import java.util.Iterator;

public class EnumerationIterator<T> implements Iterator<T>, Iterable<T> {

    Enumeration<T> enmueration;

    public EnumerationIterator(Enumeration<T> enmueration){
        this.enmueration = enmueration;
    }

    public boolean hasNext(){
        return enmueration.hasMoreElements();
    }

    public T next(){
        return enmueration.nextElement();
    }

    public void remove(){
        throw new UnsupportedOperationException();
    }

	@Override
	public Iterator<T> iterator() {
		return this;
	}
}
