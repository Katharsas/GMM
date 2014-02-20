package gmm.domain;

import gmm.service.converters.UserReferenceConverter;

import com.thoughtworks.xstream.annotations.XStreamConverter;

public class Comment extends UniqueObject {
	
	//Variables
	@XStreamConverter(UserReferenceConverter.class)
	private User author;
	private String text="";
	
	//Methods
	public Comment(User author) {
		super();
		this.author = author;
	}
	
	public Comment(User author, String text) {
		super();
		this.author = author;
		this.text = text;
	}

	//Setters, Getters
	public void setTest(String text) {
		if (text==null) throw new NullPointerException();
		this.text = text;
	}
	public String getText() {
		return text;
	}
	public User getAuthor() {
		return author;
	}
}
