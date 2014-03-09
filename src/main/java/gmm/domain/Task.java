package gmm.domain;

import gmm.service.converters.UserReferenceConverter;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;

public abstract class Task extends NamedObject{
	
	//Variables--------------------------------------------------
	@XStreamAsAttribute
	private String name;
	@XStreamConverter(UserReferenceConverter.class)
	private User author;
	@XStreamConverter(UserReferenceConverter.class)
	private User assigned = null;
	private String details = "";
	private String label="";
	@XStreamAsAttribute
	private Priority priority = Priority.MID;
	@XStreamAsAttribute
	private TaskStatus taskStatus = TaskStatus.TODO;
	
	final private List<Comment> comments = new LinkedList<Comment>();
	final private List<Task> dependsOn = new LinkedList<Task>();
	final private List<Task> dependencyFor = new LinkedList<Task>();
	
	//Methods--------------------------------------------------------
	public Task(String name, User author) {
		super(name);
		Objects.requireNonNull(author);
		this.author = author;
	}
	
	@Override
	public String toString() {
		return label+": "+name+" by "+author.toString(); 
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
	public void setPriority(Priority priority) {
		if (priority==null) throw new NullPointerException();
		this.priority = priority;
	}
	public Priority getPriority() {
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
	
	public List<Task> getDependsOn() {
		return dependsOn;
	}
	
	public List<Task> getDependencyFor() {
		return dependencyFor;
	}
}
