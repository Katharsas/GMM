package gmm.web.sessions;


import javax.annotation.PostConstruct;

import gmm.collections.Collection;
import gmm.collections.LinkedList;
import gmm.collections.List;
import gmm.domain.User;
import gmm.domain.task.Task;
import gmm.domain.task.TaskType;
import gmm.service.TaskFilterService;
import gmm.service.UserService;
import gmm.service.data.DataAccess;
import gmm.service.sort.TaskSortService;
import gmm.web.forms.FilterForm;
import gmm.web.forms.SearchForm;
import gmm.web.forms.SortForm;
import gmm.web.forms.WorkbenchLoadForm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;


/**
 * Session Bean. Gets instanciated once per session (can be used by multiple controllers).
 * Manages the session flow/logic on the Tasks page of the GMM.
 * It basically remembers the state the session is in and treats changes accordingly.
 * 
 * @author Jan
 */
@Component
@Scope(value="session", proxyMode=ScopedProxyMode.TARGET_CLASS)
public class TaskSession {

	@Autowired DataAccess data;
	@Autowired UserService users;
	@Autowired TaskFilterService filterService;
	@Autowired TaskSortService sortService;
	
	//user logged into this session
	private User user;
	
	//task filtered by general filter (base for search)
	private List<Task> filteredTasks;
	
	//filteredTasks additionally filtered by search
	private List<Task> tasks;
	
	//current settings for general filter
	private FilterForm generalFilter;
	
	//current task sort settings
	private SortForm sort;
	
	//current task load settings
	private WorkbenchLoadForm load;
	
	//triggers a data reload when set to true
	private boolean dirtyTasksFlag = false;
	
	@PostConstruct
	private void init() {
		user = users.getLoggedInUser();
		sort = new SortForm();
		load = user.getLoadForm();
		if (load == null) updateLoad(new WorkbenchLoadForm());
		initializeWorkbench();
	
		generalFilter = new FilterForm();
		updateAndFilterTasks(load.getSelected());
	}
	
	private void initializeWorkbench() {
		if (load.isReloadOnStartup()) {
			if (load.getDefaultStartupType().equals(WorkbenchLoadForm.TYPE_NONE)) {}//load nothing
			else {//load default type
				TaskType type = TaskType.valueOf(load.getDefaultStartupType());
				load.setSelected(type);
			}
		} else {
			//TODO load task list from last time
		}
	}
	
	/*--------------------------------------------------
	 * Retrieve session information
	 * ---------------------------------------------------*/
	
	public List<Task> getTasks() {
		if (dirtyTasksFlag) {
			updateAndFilterTasks(load.getSelected());
			dirtyTasksFlag = false;
		}
		return (List<Task>) tasks.copy();
	}
	
	public TaskType getCurrentTaskType() {
		return load.getSelected();
	}
	
	public User getUser() {
		return user;
	}
	
	public SortForm getSortForm() {
		return sort;
	}
	
	public FilterForm getFilterForm() {
		return generalFilter;
	}
	
	/*--------------------------------------------------
	 * Update session information
	 * ---------------------------------------------------*/
	
	/**
	 * Notify that task data will change or changed.
	 * Triggers a task data relead and refiltering on the next call of {@link #getTasks()}.
	 */
	public void notifyDataChange(){
		dirtyTasksFlag = true;
	}
	
	/**
	 * Change/update loaded tasks
	 */
	public void loadTasks(TaskType tab) {
		if (!tab.equals(load.getSelected())) {
			load.setSelected(tab);
			updateAndFilterTasks(tab);
		}
	}
	
	/**
	 * Update load settings
	 */
	public void updateLoad(WorkbenchLoadForm load) {
		this.load = load;
		getUser().setLoadForm(load);
	}
	
	/**
	 * Apply/update filtering
	 */
	public void updateFilter(FilterForm filter) {
		generalFilter = filter;
		updateAndFilterTasks(load.getSelected());
	}
	
	/**
	 * Apply/update search filtering
	 */
	public void updateSearch(SearchForm search) {
		tasks = filterService.search(filteredTasks, search);
	}
	
	/**
	 * Apply/update task sorting settings
	 */
	public void updateSort(SortForm sort) {
		this.sort = sort;
		tasks = sortService.sort(tasks, sort);
	}
	
	/**
	 * Remove task from session data.
	 */
	public void remove(Task task) {
		filteredTasks.remove(task);
		tasks.remove(task);
	}
	
	/**
	 * Add task to session data
	 */
	public void add(Task task) {
		List<Task> single = new LinkedList<>();
		single.add(task);
		filteredTasks.addAll(filterService.filter(single, generalFilter, user));
		filteredTasks = sortService.sort(filteredTasks, sort);
		tasks = (List<Task>) filteredTasks.copy();
	}
	
	/*--------------------------------------------------
	 * Private Helper methods
	 * ---------------------------------------------------*/
	
	private void updateAndFilterTasks(TaskType tab) {
		filteredTasks = new LinkedList<Task>();
		filteredTasks.addAll(filterService.filter(getTaskList(tab), generalFilter, user));
		filteredTasks = sortService.sort(filteredTasks, sort);
		tasks = (List<Task>) filteredTasks.copy();
	}
	
	private Collection<? extends Task> getTaskList (TaskType tab) {
		return data.getList(tab.toClass());
	}
}
