package gmm.web.forms;

import gmm.domain.TaskPriority;
import gmm.domain.TaskStatus;
import gmm.domain.TaskType;
import gmm.domain.User;


public class TaskForm {
	
	private String idName;
	private String label;
	private String details;
	private String assigned;
	private TaskPriority priority;
	private TaskStatus status;
	private TaskType type;
	private String originalAssetPath;
	private String newAssetFolderPath;
	
	public TaskForm() {
		setDefaultState();
	}
	public void setDefaultState() {
		idName = "";
		label = "";
		details = "";
		assigned = "";
		priority = TaskPriority.MID;
		status = TaskStatus.TODO;
		type = TaskType.GENERAL;
		originalAssetPath = "";
		newAssetFolderPath = "";
	}
	
	//Setters, Getters-------------------------------------------
	public String getIdName() {
		return idName;
	}
	public void setIdName(String idName) {
		this.idName = idName;
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
	public String getOriginalAssetPath() {
		return originalAssetPath;
	}
	public void setOriginalAssetPath(String assetPath) {
		this.originalAssetPath = assetPath;
	}
	public String getNewAssetFolderPath() {
		return newAssetFolderPath;
	}
	public void setNewAssetFolderPath(String newAssetFolderPath) {
		this.newAssetFolderPath = newAssetFolderPath;
	}
}
