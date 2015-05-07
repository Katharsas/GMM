package gmm.web.forms;

import gmm.service.sort.TaskSortAttribute;

public class SortForm {
	private TaskSortAttribute sortByPrimary;
	private TaskSortAttribute sortBySecondary;
	private boolean sortDownPrimary;
	private boolean sortDownSecondary;
	
	public SortForm() {
		setDefaultState();
	}
	public void setDefaultState() {
		sortByPrimary = TaskSortAttribute.CREATED;
		sortDownPrimary = false;
		sortBySecondary = TaskSortAttribute.TITLE;
		sortDownSecondary = true;
	}
	
	//Setters, Getters-------------------------------------------
	public TaskSortAttribute getSortByPrimary() {
		return sortByPrimary;
	}
	public void setSortByPrimary(TaskSortAttribute sortByPrimary) {
		this.sortByPrimary = sortByPrimary;
	}
	public TaskSortAttribute getSortBySecondary() {
		return sortBySecondary;
	}
	public void setSortBySecondary(TaskSortAttribute sortBySecondary) {
		this.sortBySecondary = sortBySecondary;
	}
	public boolean isSortDownPrimary() {
		return sortDownPrimary;
	}
	public void setSortDownPrimary(boolean sortDownPrimary) {
		this.sortDownPrimary = sortDownPrimary;
	}
	public boolean isSortDownSecondary() {
		return sortDownSecondary;
	}
	public void setSortDownSecondary(boolean sortDownSecondary) {
		this.sortDownSecondary = sortDownSecondary;
	}
}
