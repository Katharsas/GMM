package gmm.web.forms;

import java.util.Objects;

import gmm.domain.task.TaskType;

/**
 * Load tab settings from workbench
 * 
 * @author Jan Mothes
 */
public class LoadForm implements Form {
	
	private LoadOperation loadOperation;
	
	/**
	 * true => load defaultStartupType
	 * false => don't load anything (useful on weak client devices to only check
	 *   notifications for example)
	 * Notification:
	 *   Removed feature "load list from last login" because this goes against
	 *   the purpose of the list to always show new tasks, also no description
	 *   of that functionality for user => not intuitive, and hard to implement.
	 */
	private boolean reloadOnStartup;
	private TaskType defaultStartupType;
	
	public LoadForm() {
		setDefaultState();
	}
	
	public void setDefaultState() {
		loadOperation = LoadOperation.ONLY;
		reloadOnStartup = true;
		defaultStartupType = TaskType.GENERAL;
	}
	
	/**
	 * ADD => add tasks of selected type to workbench
	 * REMOVE => remove tasks of selected type from workbench
	 * ONLY => clear workbench, then add tasks of selected type
	 * @author Jan Mothes
	 */
	public static enum LoadOperation {
		ONLY, ADD, REMOVE
	}
	
	//Setters, Getters-------------------------------------------

	public LoadOperation getLoadOperation() {
		return loadOperation;
	}
	public void setLoadOperation(LoadOperation loadOperation) {
		Objects.requireNonNull(loadOperation);
		this.loadOperation = loadOperation;
	}
	public boolean isReloadOnStartup() {
		return reloadOnStartup;
	}
	public void setReloadOnStartup(boolean reloadOnStartup) {
		this.reloadOnStartup = reloadOnStartup;
	}
	public TaskType getDefaultStartupType() {
		return defaultStartupType;
	}
	public void setDefaultStartupType(TaskType defaultStartupType) {
		Objects.requireNonNull(defaultStartupType);
		this.defaultStartupType = defaultStartupType;
	}
}
