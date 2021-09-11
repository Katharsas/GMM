package gmm.web.forms;

import gmm.domain.task.TaskPriority;
import gmm.domain.task.TaskStatus;
import gmm.domain.task.TaskType;

/**
 * Multithreading-safe.
 */
public class AssetTaskTemplateForm implements Form {
	
	private String name;
	private String label;
	private String details;
	private String assigned;
	private TaskPriority priority;
	private TaskStatus statusNoNewAsset;
	private TaskStatus statusWithNewAsset;
	
	public AssetTaskTemplateForm() {
		setDefaultState();
	}
	public void setDefaultState() {
		name = "%filename%";
		label = "";
		details = "";
		assigned = "";
		priority = TaskPriority.MID;
		statusNoNewAsset = TaskStatus.TODO;
		statusWithNewAsset = TaskStatus.INREVIEW;
	}
	
	public TaskForm createTaskForm(TaskType type, String assetName, boolean existsNewAsset) {
		TaskForm result = new TaskForm();
		
		result.setName(name.replace("%filename%", assetName));
		result.setDetails(details.replace("%filename%", assetName));
		
		result.setLabel(assigned);
		result.setAssigned(assigned);
		result.setPriority(priority);
		result.setLabel(label);
		
		result.setStatus(existsNewAsset ? statusWithNewAsset : statusNoNewAsset);
		result.setType(type);
		result.setAssetName(assetName);
		
		return result;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
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
	public String getDetails() {
		return details;
	}
	public void setDetails(String details) {
		this.details = details;
	}
	public String getAssigned() {
		return assigned;
	}
	public void setAssigned(String assigned) {
		this.assigned = assigned;
	}
	public TaskPriority getPriority() {
		return priority;
	}
	public void setPriority(TaskPriority priority) {
		this.priority = priority;
	}
	public TaskStatus getStatusNoNewAsset() {
		return statusNoNewAsset;
	}
	public void setStatusNoNewAsset(TaskStatus statusNoNewAsset) {
		this.statusNoNewAsset = statusNoNewAsset;
	}
	public TaskStatus getStatusWithNewAsset() {
		return statusWithNewAsset;
	}
	public void setStatusWithNewAsset(TaskStatus statusWithNewAsset) {
		this.statusWithNewAsset = statusWithNewAsset;
	}
}
