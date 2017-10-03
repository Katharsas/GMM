package gmm.domain;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

public class TaskNotification extends Notification {

	@XStreamAsAttribute
	private String taskIdLink;
	
	public TaskNotification(String text, String taskIdLink) {
		super(text);
		this.taskIdLink = taskIdLink;
	}
	
	public String getTaskIdLink() {
		return taskIdLink;
	}
}
