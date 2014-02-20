package gmm.domain;


public class Notification extends UniqueObject{

	private String text;
	
	public Notification(String text) {
		super();
		this.text = text;
	}
	
	//Setters, Getters
	public String getText() {
		return text;
	}
}
