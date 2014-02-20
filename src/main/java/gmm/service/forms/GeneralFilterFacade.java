package gmm.service.forms;

import gmm.domain.Priority;
import gmm.domain.TaskStatus;
import gmm.util.ListUtil;


public class GeneralFilterFacade {
	private boolean createdByMe;
	private boolean assignedToMe;
	private boolean all;
	private boolean hidden;
	private Boolean[] priority;
	private Boolean[] taskStatus;

	public GeneralFilterFacade() {
		setDefaultState();
	}
	public void setDefaultState() {
		createdByMe = false;
		assignedToMe = false;
		all = true;
		hidden = true;
		priority = ListUtil.inflateToArray(true, Priority.values().length);
		taskStatus = ListUtil.inflateToArray(true, TaskStatus.values().length);
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
	public boolean isHidden() {
		return hidden;
	}
	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}
}
