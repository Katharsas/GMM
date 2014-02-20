package gmm.domain;


import java.util.Collection;
import java.util.Date;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

public abstract class UniqueObject implements Linkable{
	
	//Variables-------------------------------------------------
	private static long idCount = 0;
	@XStreamAsAttribute
	private long id;
	private Date created;
	
	//Methods---------------------------------------------------
	public UniqueObject() {
		id = ++idCount;
		created = new Date();
	}
	
	@Override
	public String getIdLink() {
		return id+"";
	}
	
	public  static <U extends UniqueObject> U getFromId(Collection<U> c, String idLink) {
		if (c==null || idLink==null) throw new NullPointerException();
		for(U u : c) {
			if(u.getIdLink().equals(idLink)) return u;
		}
		return null;
	}
	
	@Override
	public boolean equals(Object o) {
		if((o!=null) && o instanceof UniqueObject){
			UniqueObject p = (UniqueObject) o;
    		return getIdLink().equals(p.getIdLink());
    	}
    	return false;
	}
	

	//Setters, Getters----------------------------------------------
	public Date getCreationDate() {
		return created;
	}
}
