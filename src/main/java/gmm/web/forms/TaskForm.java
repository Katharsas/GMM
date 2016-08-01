package gmm.web.forms;

import gmm.domain.User;
import gmm.domain.task.TaskPriority;
import gmm.domain.task.TaskStatus;
import gmm.domain.task.TaskType;


public class TaskForm implements Form {
	
	private String name;
	private String label;
	private String details;
	private String assigned;
	private TaskPriority priority;
	private TaskStatus status;
	private TaskType type;
	private String assetPath;
	
	public TaskForm() {
		setDefaultState();
	}
	public void setDefaultState() {
		name = "";
		label = "";
		details = "";
		assigned = "";
		priority = TaskPriority.MID;
		status = TaskStatus.TODO;
		type = TaskType.GENERAL;
		assetPath = "";
	}
	
	//Setters, Getters-------------------------------------------
	public String getName() {
		return name;
	}
	public void setName(String idName) {
		this.name = idName;
	}
	public String getDetails() {
		return details;
	}
	public void setDetails(String details) {
		this.details = details;
	}
	public TaskPriority getPriority() {
		return priority;
	}
	public void setPriority(TaskPriority priority) {
		this.priority = priority;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public String getLabelSelect() {
		return label;
	}
	public void setLabelSelect(String label) {
		if(this.label.equals("")) this.label = label;
	}
	public TaskStatus getStatus() {
		return status;
	}
	public void setStatus(TaskStatus status) {
		this.status = status;
	}
	public String getAssigned() {
		return assigned;
	}
	public void setAssigned(String assigned) {
		this.assigned = assigned;
	}
	public void setAssigned(User user) {
		this.assigned = user==null ? "" : user.getName();
	}
	public TaskType getType() {
		return type;
	}
	public void setType(TaskType type) {
		this.type = type;
	}
	public String getAssetPath() {
		return assetPath;
	}
	public void setAssetPath(String assetPath) {
		this.assetPath = assetPath;
	}
}
