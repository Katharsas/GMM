package gmm.domain;


import java.util.Collection;
import java.util.Objects;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

public abstract class NamedObject extends UniqueObject{

	@XStreamAsAttribute
	private String name;
	
	public NamedObject(String name) {
		setName(name);
	}
	
	/**
	 * Returns the first object from a collection which has the same name as this object.
	 * @param c
	 * @param name
	 * @param thisName
	 * @return
	 */
	public static <N extends NamedObject> N getFromName(Collection<N> c, String name) {
		Objects.requireNonNull(c);
		for(N n : c) {
			if(n.getName().equals(name)) return n;
		}
		return null;
	}
	
	@Override
	public boolean equals(Object o) {
		if((o!=null) && o instanceof NamedObject){
			NamedObject p = (NamedObject) o;
    		return getName().equals(p.getName());
    	}
    	return false;
	}

	public String getName() {
		return name;
	}

	public void setName(String idName) {
		Objects.requireNonNull(idName);
		this.name = idName;
	}
	
	@Override
	public String toString() {
		return getName();
	}
}
