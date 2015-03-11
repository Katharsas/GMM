package gmm.web.forms;

import java.util.Objects;

import org.apache.commons.lang3.EnumUtils;

import gmm.domain.task.TaskType;

/**
 * Load tab from workbench
 * 
 * @author Jan Mothes
 */
public class WorkbenchLoadForm {
	
	private TaskType selected;//only for highlighting button
	private LoadOperation loadOperation;
	
	private boolean reloadOnStartup;
	//true => use operation to load default startup type
	//false => load tasks which where in workbench last time
	private String defaultStartupType;//basically NONE or TaskType enum value
	final public static String TYPE_NONE = "NONE";
	
	public WorkbenchLoadForm() {
		setDefaultState();
	}
	
	public void setDefaultState() {
		selected = TaskType.GENERAL;
		loadOperation = LoadOperation.ONLY;
		reloadOnStartup = true;
		defaultStartupType = TaskType.GENERAL.name();
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

	public TaskType getSelected() {
		return selected;
	}
	public void setSelected(TaskType selected) {
		Objects.requireNonNull(selected);
		this.selected = selected;
	}
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
	public String getDefaultStartupType() {
		return defaultStartupType;
	}
	public void setDefaultStartupType(String defaultStartupType) {
		Objects.requireNonNull(defaultStartupType);
		boolean isType = EnumUtils.isValidEnum(TaskType.class, defaultStartupType);
		if (!(isType || defaultStartupType.equals(TYPE_NONE))) {
			throw new IllegalArgumentException("defaultStartupType must be TaskType or '"+TYPE_NONE+"'");
		}
		this.defaultStartupType = defaultStartupType;
	}
}
