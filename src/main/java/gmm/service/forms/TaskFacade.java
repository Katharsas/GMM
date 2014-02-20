package gmm.service.forms;

import gmm.domain.Priority;
import gmm.domain.TaskStatus;
import gmm.domain.User;


public class TaskFacade {
	
	private String idName;
	private String label;
	private String details;
	private String assigned;
	private Priority priority;
	private TaskStatus status;
	
	public TaskFacade() {
		setDefaultState();
	}
	public void setDefaultState() {
		idName="";
		label="";
		details="";
		assigned="";
		priority=Priority.MID;
		status=TaskStatus.TODO;
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
	public Priority getPriority() {
		return priority;
	}
	public void setPriority(Priority priority) {
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
}
