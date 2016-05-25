package gmm.web.sessions;


import java.util.Arrays;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import gmm.collections.Collection;
import gmm.collections.LinkedList;
import gmm.collections.List;
import gmm.domain.User;
import gmm.domain.task.Task;
import gmm.domain.task.TaskType;
import gmm.service.TaskFilterService;
import gmm.service.data.DataAccess;
import gmm.service.data.DataAccess.TaskUpdateCallback;
import gmm.service.sort.TaskSortService;
import gmm.service.users.UserService;
import gmm.util.Util;
import gmm.web.forms.FilterForm;
import gmm.web.forms.LoadForm;
import gmm.web.forms.LoadForm.LoadOperation;
import gmm.web.forms.SearchForm;
import gmm.web.forms.SortForm;


/**
 * Session Bean. Gets instanciated once per session (can be used by multiple controllers).
 * Manages the session flow/logic on the Tasks page of the GMM.
 * It basically remembers the state the session is in and treats changes accordingly.
 * 
 * @author Jan Mothes
 */
@Component
@Scope(value="session", proxyMode=ScopedProxyMode.TARGET_CLASS)
public class WorkbenchSession {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	/**
	 * TODO: Actually push this stuff to client
	 * TODO: make threadsafe ?!
	 * 
	 * Updates session caches when task data changes.
	 */
	private class UpdateCallback implements TaskUpdateCallback {
		@Override
		public <T extends Task> void onAdd(T task) {
			final List<T> single = new LinkedList<T>(Util.classOf(task), task);
			onAddAll(single);
		}
		@Override
		public <T extends Task> void onAddAll(Collection<T> tasks) {
			final TaskType type = TaskType.fromClass(tasks.getGenericType());
			if(selected[type.ordinal()]) {
				filteredTasks.addAll(filter(tasks));
				sort(filteredTasks);
				WorkbenchSession.this.tasks = filteredTasks.copy();
				notifyClient();
			}
		}
		@Override
		public <T extends Task> void onRemove(T task) {
			final List<T> single = new LinkedList<T>(Util.classOf(task), task);
			onRemoveAll(single);
		}
		@Override
		public <T extends Task> void onRemoveAll(Collection<T> tasks) {
			filteredTasks.removeAll(tasks);
			WorkbenchSession.this.tasks.removeAll(tasks);
			possiblyDirtyTasks.addAll(tasks);
			notifyClient();
		}
		private void notifyClient() {
			
		}
	}
	
	@Autowired private DataAccess data;
	@Autowired private UserService users;
	@Autowired private TaskFilterService filterService;
	@Autowired private TaskSortService sortService;
	
	//user logged into this session
	private User user;
	
	//currently active task lists (any of TaskType)
	private boolean[] selected = new boolean[TaskType.values().length];
	
	//task filtered by general filter (base for search)
	private List<Task> filteredTasks;
	
	//filteredTasks additionally filtered by search
	private List<Task> tasks;
	
	//remember removed tasks in case they get replaced by tasks with same id
	private List<Task> possiblyDirtyTasks;
	
	//current settings for general filter
	private FilterForm generalFilter;
	
	//current task sort settings
	private SortForm sort;
	
	//current task load settings
	private LoadForm load;
	
	//needed to prevent from getting garbage collected
	//needs to die with this object
	private final UpdateCallback strongReference;
	
	public WorkbenchSession() {
		 strongReference = new UpdateCallback();
	}
	
	@PostConstruct
	private void init() {
		data.registerForUpdates(strongReference);
		
		filteredTasks = new LinkedList<>(Task.class);
		tasks = new LinkedList<>(Task.class);
		possiblyDirtyTasks = new LinkedList<>(Task.class);
		
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
		final int i = type.ordinal();
		if(!selected[i]) {
			filteredTasks.addAll(filter(getTaskList(type)));
			sort(filteredTasks);
			selected[i] = true;
		}
		tasks = filteredTasks.copy();
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
		final int i = type.ordinal();
		if(selected[i]) {
			final Collection<? extends Task> toRemove = getTaskList(type);
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
		final TaskType[] types = TaskType.values();
		for(int i = 0; i < selected.length; i++) {
			if(selected[i]) {
				filteredTasks.addAll(filter(getTaskList(types[i])));
			}
		}
		sort(filteredTasks);
		tasks = filteredTasks.copy();
	}
	
	private <T extends Task> Collection<T> filter(Collection<T> tasks) {
		return filterService.filter(tasks, generalFilter, user);
	}
	
	private <T extends Task> void sort(List<T> tasks) {
		sortService.sort(tasks, sort);
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
		sort(tasks);
	}
	
	/*--------------------------------------------------
	 * Retrieve session information
	 * ---------------------------------------------------*/
	
	public List<Task> getTasks() {
		return tasks.copy();
	}
	
	/**
	 * Getting this list will clear it, you can only get it once!
	 */
	public List<Task> getDirtyTasks() {
		final List<Task> result = possiblyDirtyTasks.copy();
		possiblyDirtyTasks.clear();
		return result;
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
