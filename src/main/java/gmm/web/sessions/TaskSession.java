package gmm.web.sessions;


import java.util.Arrays;

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
import gmm.web.forms.LoadForm;
import gmm.web.forms.LoadForm.LoadOperation;

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
	
	//currently active task lists (any of TaskType)
	private boolean[] selected = new boolean[TaskType.values().length];
	
	//task filtered by general filter (base for search)
	private List<Task> filteredTasks;
	
	//filteredTasks additionally filtered by search
	private List<Task> tasks;
	
	//current settings for general filter
	private FilterForm generalFilter;
	
	//current task sort settings
	private SortForm sort;
	
	//current task load settings
	private LoadForm load;
	
	//triggers a data reload when set to true
	private boolean dirtyTasksFlag = false;
	
	@PostConstruct
	private void init() {
		filteredTasks = new LinkedList<Task>();
		tasks = new LinkedList<Task>();
		sort = new SortForm();
		generalFilter = new FilterForm();
		
		user = users.getLoggedInUser();
		load = user.getLoadForm();
		if (load == null) updateLoad(new LoadForm());
		if (load.isReloadOnStartup()) {
			load(load.getDefaultStartupType(), LoadOperation.ONLY);
		}
	}
	
	/*--------------------------------------------------
	 * Private Helper methods
	 * ---------------------------------------------------*/
	
	private void load(TaskType type, LoadOperation operation) {
		switch (operation) {
		case ADD:
			add(type);break;
		case ONLY:
			only(type);break;
		case REMOVE:
			remove(type);break;
		}
	}
	
	/**
	 * Add a type of tasks to the currently selected types. Will reset modifiers
	 * like search.
	 */
	private void add(TaskType type) {
		int i = type.ordinal();
		if(!selected[i]) {
			filteredTasks.addAll(filter(getTaskList(type)));
			filteredTasks = sort(filteredTasks);
			selected[i] = true;
		}
		tasks = (List<Task>) filteredTasks.copy();
	}
	
	/**
	 * Select only one specific type of tasks. Will reset modifiers like search.
	 */
	private void only(TaskType type) {
		filteredTasks.clear();
		Arrays.fill(selected, false);
		add(type);
	}
	
	/**
	 * Remove a type of tasks from the currently selected. Will NOT reset modifiers
	 * like search.
	 */
	private void remove(TaskType type) {
		int i = type.ordinal();
		if(selected[i]) {
			Collection<? extends Task> toRemove = getTaskList(type);
			filteredTasks.removeAll(toRemove);
			selected[i] = false;
			tasks.removeAll(toRemove);
		}
	}
	
	/**
	 * Reloads all tasks. Will reset modifiers like search.
	 */
	private void reload() {
		filteredTasks.clear();
		TaskType[] types = TaskType.values();
		for(int i = 0; i < selected.length; i++) {
			if(selected[i]) {
				filteredTasks.addAll(filter(getTaskList(types[i])));
			}
		}
		filteredTasks = sort(filteredTasks);
		tasks = (List<Task>) filteredTasks.copy();
	}
	
	private <T extends Task> Collection<T> filter(Collection<T> tasks) {
		return filterService.filter(tasks, generalFilter, user);
	}
	
	private <T extends Task> List<T> sort(List<T> tasks) {
		return sortService.sort(tasks, sort);
	}
	
	private Collection<? extends Task> getTaskList (TaskType type) {
		return data.getList(type.toClass());
	}
	
	/*--------------------------------------------------
	 * Update session information
	 * ---------------------------------------------------*/
	
	/**
	 * Notify that task data will change or changed.
	 * Triggers a task data reload and refiltering on the next call of {@link #getTasks()}.
	 */
	public void notifyDataChange(){
		dirtyTasksFlag = true;
	}
	
	/**
	 * Change/update loaded tasks
	 */
	public void loadTasks(TaskType type) {
		load(type, load.getLoadOperation());
	}
	
	/**
	 * Update load settings
	 */
	public void updateLoad(LoadForm load) {
		synchronized (load) {
			this.load = load;
			getUser().setLoadForm(load);
		}
	}
	
	/**
	 * Apply/update filtering
	 */
	public void updateFilter(FilterForm filter) {
		generalFilter = filter;
		filteredTasks = (List<Task>) filter(filteredTasks);
		tasks = (List<Task>) filteredTasks.copy();
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
		tasks = sort(tasks);
	}
	
	/**
	 * Remove task from session data.
	 */
	public void remove(Task task) {
		filteredTasks.remove(task);
		tasks.remove(task);
	}
	
	/**
	 * Add task to session data. Resets modifiers like search.
	 */
	public void add(Task task) {
		TaskType type = TaskType.fromClass(task.getClass());
		if(selected[type.ordinal()]) {
			List<Task> single = new LinkedList<>();
			single.add(task);
			filteredTasks.addAll(filter(single));
			filteredTasks = sort(filteredTasks);
			tasks = (List<Task>) filteredTasks.copy();
		}
	}
	
	/*--------------------------------------------------
	 * Retrieve session information
	 * ---------------------------------------------------*/
	
	public List<Task> getTasks() {
		if(dirtyTasksFlag) {
			reload();
			dirtyTasksFlag = false;
		}
		return (List<Task>) tasks.copy();
	}
	
	public boolean[] getSelectedTaskTypes() {
		return selected;
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
}
