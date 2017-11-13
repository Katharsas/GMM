package gmm.domain;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import gmm.domain.task.Task;
import gmm.service.data.DataChangeType;

public class TaskNotification extends Notification {

	@XStreamAsAttribute
	private final String taskIdLink;
	@XStreamAsAttribute
	private final String taskName;
	@XStreamAsAttribute
	private final DataChangeType changeType;
	@XStreamAsAttribute
	private final String userName;
	
	public TaskNotification(Task task, DataChangeType changeType, User source) {
		this(task.getIdLink(), task.getName(), changeType, source.getName());
	}
	
	public TaskNotification(String taskIdLink, String taskName, DataChangeType changeType, String userName) {
		super("Task '" + taskName + "' was '" + changeType.name()+ "' by '" + userName + "'.");
		this.taskIdLink = taskIdLink;
		this.taskName = taskName;
		this.changeType = changeType;
		this.userName = userName;
	}
	
	public String getTaskIdLink() {
		return taskIdLink;
	}
	
	public String getTaskName() {
		return taskName;
	}
	
	public String getChangeType() {
		return changeType.name();
	}
	
	public String getUserName() {
		return userName;
	}
}
