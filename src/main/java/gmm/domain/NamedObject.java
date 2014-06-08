package gmm.domain;


import java.util.Collection;
import java.util.Objects;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

public abstract class NamedObject extends UniqueObject{

	@XStreamAsAttribute
	private String name = "";
	
	public NamedObject() {
		super();
	}
	
	public NamedObject(String name) {
		super();
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
