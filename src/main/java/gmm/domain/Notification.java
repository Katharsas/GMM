package gmm.domain;

import java.util.Objects;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

public class Notification extends UniqueObject {

	@XStreamAsAttribute
	private String text;
	
	public Notification(String text) {
		super();
		Objects.requireNonNull(text);
		this.text = text;
	}
	
	//Setters, Getters
	public String getText() {
		return text;
	}
}
