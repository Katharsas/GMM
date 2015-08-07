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
import gmm.service.data.DataAccess.TaskUpdateCallback;
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
 * @author Jan Mothes
 */
@Component
@Scope(value="session", proxyMode=ScopedProxyMode.TARGET_CLASS)
public class TaskSession {

	/**
	 * TODO: Actually push this stuff to client
	 * 
	 * Updates session caches when task data changes.
	 */
	private class UpdateCallback implements TaskUpdateCallback {
		@Override
		public <T extends Task> void onAdd(T task) {
			List<T> single = new LinkedList<>();
			single.add(task);
			@SuppressWarnings("unchecked")
			Class<T> clazz = (Class<T>) task.getClass();
			onAddAll(single, clazz);
		}
		@Override
		public <T extends Task> void onAddAll(Collection<T> tasks, Class<T> clazz) {
			TaskType type = TaskType.fromClass(clazz);
			if(selected[type.ordinal()]) {
				filteredTasks.addAll(filter(tasks));
				filteredTasks = sort(filteredTasks);
				TaskSession.this.tasks = filteredTasks.copy();
			}
		}
		@Override
		public <T extends Task> void onRemove(T task) {
			filteredTasks.remove(task);
			tasks.remove(task);
		}
		@Override
		public <T extends Task> void onRemoveAll(Collection<T> tasks) {
			filteredTasks.removeAll(tasks);
			tasks.removeAll(tasks);
		}
	}
	
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
	
	//needed to prevent from getting garbage collected
	//needs to die with this object
	private UpdateCallback strongReference;
	
	@PostConstruct
	private void init() {
		strongReference = new UpdateCallback();
		data.registerForUpdates(strongReference);
		
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
		reload();
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
	
	/*--------------------------------------------------
	 * Retrieve session information
	 * ---------------------------------------------------*/
	
	public List<Task> getTasks() {
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
