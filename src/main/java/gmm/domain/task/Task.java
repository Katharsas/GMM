package gmm.domain.task;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import gmm.domain.Comment;
import gmm.domain.NamedObject;
import gmm.domain.User;

public abstract class Task extends NamedObject {
	
	//Static--------------------------------------------------
	
	private static SecureRandom random = new SecureRandom();
	private static String getRandomKey() {
		//toString(32) encodes 5 bits/char, so BigInteger range bits should be a multiple of 5
		return new BigInteger(70, random).toString(32);
	}
	
	//Variables--------------------------------------------------
	/** XStream depends on "author" variable name {@link gmm.service.data.xstream.XMLService}
	 */
	private final User author;
	/** XStream depends on "assigned" variable name {@link gmm.service.data.xstream.XMLService}
	 */
	private User assigned = null;
	private String details = "";
	private String label="";
	@XStreamAsAttribute
	private TaskPriority priority = TaskPriority.MID;
	@XStreamAsAttribute
	private TaskStatus taskStatus = TaskStatus.TODO;
	
	private final String linkKey = Task.getRandomKey();
	
	private final List<Comment> comments = new LinkedList<Comment>();
	private final List<Task> dependsOn = new LinkedList<Task>();
	private final List<Task> dependencyFor = new LinkedList<Task>();
	
	//Methods--------------------------------------------------------
	protected Task() {
		this.author = null;
	}
	
	public Task(User author) {
		super();
		Objects.requireNonNull(author);
		this.author = author;
	}
	
	@Override
	public String toString() {
		return "[" + getId() + ": \"" + getName() + "\" by " + author.toString() + "]"; 
	}
	//Setters, Getters-------------------------------------------
	public void setDetails(String details) {
		if (details==null) throw new NullPointerException();
		this.details = details;
	}
	public String getDetails() {
		return details;
	}
	public void setLabel(String label) {
		if (label==null) throw new NullPointerException();
		this.label = label;
	}
	public String getLabel() {
		return label;
	}
	public void setPriority(TaskPriority priority) {
		if (priority==null) throw new NullPointerException();
		this.priority = priority;
	}
	public TaskPriority getPriority() {
		return priority;
	}
	public void setTaskStatus(TaskStatus taskStatus) {
		if (taskStatus==null) throw new NullPointerException();
		this.taskStatus = taskStatus;
	}
	public TaskStatus getTaskStatus() {
		return taskStatus;
	}
	public List<Comment> getComments() {
		return comments;
	}
	public User getAuthor() {
		return author;
	}
	public User getAssigned() {
		return assigned;
	}
	public void setAssigned(User assigned) {
		this.assigned = assigned;
	}
	public String getLinkKey() {
		return linkKey;
	}
	
	public List<Task> getDependsOn() {
		return dependsOn;
	}
	
	public List<Task> getDependencyFor() {
		return dependencyFor;
	}

	@Deprecated
	public void onLoad(){
	}
	public abstract TaskType getType();
}
