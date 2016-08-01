package gmm.web.forms;

import gmm.collections.ListUtil;
import gmm.domain.task.TaskPriority;
import gmm.domain.task.TaskStatus;


public class FilterForm implements Form {
	private boolean createdByMe;
	private boolean assignedToMe;
	private boolean all;
	private Boolean[] priority;
	private Boolean[] taskStatus;

	public FilterForm() {
		setDefaultState();
	}
	public void setDefaultState() {
		createdByMe = false;
		assignedToMe = false;
		all = true;
		priority = ListUtil.inflateToArray(true, TaskPriority.values().length);
		taskStatus = ListUtil.inflateToArray(true, TaskStatus.values().length);
	}
	public boolean isInDefaultState() {
		return !createdByMe && !assignedToMe && all;
	}
	
	//Setters, Getters-------------------------------------------
	public boolean isCreatedByMe() {
		return createdByMe;
	}
	public void setCreatedByMe(boolean createdByMe) {
		this.createdByMe = createdByMe;
	}
	public boolean isAssignedToMe() {
		return assignedToMe;
	}
	public void setAssignedToMe(boolean assignedToMe) {
		this.assignedToMe = assignedToMe;
	}
	public Boolean[] getPriority() {
		return priority;
	}
	public void setPriority(Boolean[] priority) {
		this.priority = priority;
	}
	public Boolean[] getTaskStatus() {
		return taskStatus;
	}
	public void setTaskStatus(Boolean[] taskStatus) {
		this.taskStatus = taskStatus;
	}
	public boolean isAll() {
		return all;
	}
	public void setAll(boolean all) {
		this.all = all;
	}
}