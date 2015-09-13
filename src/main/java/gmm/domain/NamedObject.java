package gmm.domain;


import java.util.Objects;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

public abstract class NamedObject extends UniqueObject {

	@XStreamAsAttribute
	private String name = "";
	
	public NamedObject() {
		super();
	}
	
	public NamedObject(String name) {
		super();
		setName(name);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		Objects.requireNonNull(name);
		this.name = name;
	}
	
	@Override
	public String toString() {
		return getName();
	}
}
