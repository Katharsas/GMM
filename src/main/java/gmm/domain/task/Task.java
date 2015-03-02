package gmm.domain.task;

import gmm.domain.Comment;
import gmm.domain.NamedObject;
import gmm.domain.User;
import gmm.service.converters.UserReferenceConverter;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;

public abstract class Task extends NamedObject{
	
	//Static--------------------------------------------------
	
	private static SecureRandom random = new SecureRandom();
	public static String getRandomKey() {
		//toString(32) encodes 5 bits/char, so BigInteger range bits should be a multiple of 5
		return new BigInteger(70, random).toString(32);
	}
	
	//Variables--------------------------------------------------
	@XStreamConverter(UserReferenceConverter.class)
	final private User author;
	@XStreamConverter(UserReferenceConverter.class)
	private User assigned = null;
	private String details = "";
	private String label="";
	@XStreamAsAttribute
	private TaskPriority priority = TaskPriority.MID;
	@XStreamAsAttribute
	private TaskStatus taskStatus = TaskStatus.TODO;
	
	final private String linkKey = Task.getRandomKey();
	
	final private List<Comment> comments = new LinkedList<Comment>();
	final private List<Task> dependsOn = new LinkedList<Task>();
	final private List<Task> dependencyFor = new LinkedList<Task>();
	
	//Methods--------------------------------------------------------
	public Task(User author) throws Exception {
		super();
		Objects.requireNonNull(author);
		this.author = author;
		onLoad();
	}
	
	@Override
	public String toString() {
		return label+": "+getName()+" by "+author.toString(); 
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

	public void onLoad() throws Exception {
	}
	public abstract TaskType getType();
}
