package gmm.domain;


import java.util.Collection;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

public abstract class NamedObject extends UniqueObject{

	@XStreamAsAttribute
	private String idName;
	
	public NamedObject(String idName) {
		if (idName==null) throw new NullPointerException();
		this.idName = idName;
	}
	
	/**
	 * Returns the first object from a collection which has the same name as this object.
	 * @param c
	 * @param name
	 * @param thisName
	 * @return
	 */
	public static <N extends NamedObject> N getFromName(Collection<N> c, String name) {
		if (c==null) throw new NullPointerException();
		for(N n : c) {
			if(n.getIdName().equals(name)) return n;
		}
		return null;
	}
	
	@Override
	public boolean equals(Object o) {
		if((o!=null) && o instanceof NamedObject){
			NamedObject p = (NamedObject) o;
    		return getIdName().equals(p.getIdName());
    	}
    	return false;
	}

	public String getIdName() {
		return idName;
	}

	protected void setIdName(String idName) {
		this.idName = idName;
	}
	
	@Override
	public String toString() {
		return getIdName();
	}
}
