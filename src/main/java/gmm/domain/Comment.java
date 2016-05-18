package gmm.domain;

import java.util.Date;
import java.util.Objects;

public class Comment extends UniqueObject {
	
	//Variables
	/** XStream depends on "author" variable name {@link gmm.service.data.xstream.XMLService}
	 */
	private User author;
	private String text="";
	private Date edited;
	
	//Methods
	public Comment(User author) {
		super();
		Objects.requireNonNull(author);
		this.author = author;
	}
	
	public Comment(User author, String text) {
		this(author);
		setText(text);
	}

	//Setters, Getters
	public void setText(String text) {
		Objects.requireNonNull(text);
		this.edited = new Date();
		this.text = text;
	}
	public String getText() {
		return text;
	}
	public User getAuthor() {
		return author;
	}
	public Date getLastEditedDate() {
		return edited;
	}
}
