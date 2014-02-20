package gmm.util;

import java.lang.reflect.Array;

public class ListUtil<T> {
	
	public static <T> T[] inflateToArray (T t, int size){
		@SuppressWarnings("unchecked")
		T[] buffer = (T[]) Array.newInstance(t.getClass(), size);
		for(int i=0; i<size; i++) {
			buffer[i] = t;
		}
		return buffer;
	}
}
