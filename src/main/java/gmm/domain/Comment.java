package gmm.domain;

import java.util.Objects;

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
		Objects.requireNonNull(author);
		this.author = author;
		setText(text);
	}

	//Setters, Getters
	public void setText(String text) {
		Objects.requireNonNull(text);
		this.text = text;
	}
	public String getText() {
		return text;
	}
	public User getAuthor() {
		return author;
	}
}
