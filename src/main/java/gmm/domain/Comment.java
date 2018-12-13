package gmm.domain;

import java.util.Date;
import java.util.Objects;

public class Comment extends UniqueObject {
	
	private final User author;
	private String text;
	private Date edited;
	
	public Comment() {
		author = null;
	}
	
	public Comment(User author, String text) {
		Objects.requireNonNull(author);
		this.author = author;
		setText(text);
	}

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
