package gmm.domain;

import gmm.service.converters.UserReferenceConverter;

import java.util.LinkedList;
import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

public class Task extends UniqueObject{
	
	//Variables--------------------------------------------------
	//Domain - Set by constructor
	@XStreamAsAttribute
	private String name;
	@XStreamConverter(UserReferenceConverter.class)
	private User author;
	//Domain - Default
	private User assigned = null;
	private String details = "";
	private String label="";
	private Priority priority = Priority.MID;
	private TaskStatus taskStatus = TaskStatus.TODO;
	@XStreamImplicit
	final private List<Comment> comments = new LinkedList<Comment>();
	@XStreamImplicit(itemFieldName="dependsOn")
	final private List<Task> dependsOn = new LinkedList<Task>();
	@XStreamImplicit(itemFieldName="dependencyFor")
	final private List<Task> dependencyFor = new LinkedList<Task>();
	
	//Methods--------------------------------------------------------
	public Task(String name, User author) {
		super();
		if(name==null || author==null) throw new NullPointerException();
		this.name = name;
		this.author = author;
	}
	
	@Override
	public String toString() {
		return label+": "+name+" by "+author.toString(); 
	}
	//Setters, Getters-------------------------------------------
	public void setName(String idName) {
		if (idName==null) throw new NullPointerException();
		this.name = idName;
	}
	public String getName() {
		return name;
	}
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
