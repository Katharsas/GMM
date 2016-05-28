package gmm.web.sessions.tasklist;


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
import gmm.service.sort.TaskSortService;
import gmm.service.users.UserService;
import gmm.web.forms.FilterForm;
import gmm.web.forms.LoadForm;
import gmm.web.forms.LoadForm.LoadOperation;
import gmm.web.forms.SearchForm;
import gmm.web.forms.SortForm;


/**
 * Session Bean. Gets instantiated once per session (can be used by multiple controllers).
 * Manages the session flow/logic on the Tasks page of the GMM.
 * It basically remembers the state the session is in and treats changes accordingly.
 * 
 * @author Jan Mothes
 */
@Component
@Scope(value="session", proxyMode=ScopedProxyMode.TARGET_CLASS)
public class WorkbenchSession extends TaskListState {

	@SuppressWarnings("unused")
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired private DataAccess data;
	@Autowired private UserService users;
	@Autowired private TaskFilterService filterService;
	@Autowired private TaskSortService sortService;
	
	//user logged into this session
	private User user;
	
	//currently active task lists (any of TaskType)
	private boolean[] selected = new boolean[TaskType.values().length];
	
	//task filtered by general filter & search
	private List<Task> visible;
	
	//current settings for general filter
	private FilterForm generalFilter;
	
	//current settings for search
	private SearchForm searchFilter;
	
	//current task sort settings
	private SortForm sort;
	
	//current task load settings
	private LoadForm load;
	
	public WorkbenchSession() {
	}
	
	@PostConstruct
	private void init() {
		data.registerForUpdates(this);
		
		visible = new LinkedList<>(Task.class);
		
		sort = new SortForm();
		generalFilter = new FilterForm();
		searchFilter = new SearchForm();
		
		user = users.getLoggedInUser();
		load = user.getLoadForm();
		if (load == null) updateLoad(new LoadForm());
		if (load.isReloadOnStartup()) {
			load(load.getDefaultStartupType(), LoadOperation.ONLY);
		}
	}
	
	@Override
	boolean isTaskTypeVisible(TaskType type) {
		return selected[type.ordinal()];
	}

	@Override
	List<Task> getVisible() {
		return visible;
	}

	@Override
	<T extends Task> Collection<T> filter(Collection<T> tasks) {
		Collection<T> filtered = filterService.filter(tasks, generalFilter, user);
		return filterService.search(filtered, searchFilter);
	}

	@Override
	void sortVisible() {
		sortService.sort(visible, sort);
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
		taskListEvents.add(new TaskListEvent.FilterAll(getIds(visible)));
	}
	
	/**
	 * Add a type of tasks to the currently selected types. Will reset modifiers
	 * like search.
	 */
	private void add(TaskType type) {
		final int i = type.ordinal();
		if(!selected[i]) {
			visible.addAll(filter(getTaskList(type)));
			sortVisible();
			selected[i] = true;
		}
	}
	
	/**
	 * Select only one specific type of tasks. Will reset modifiers like search.
	 */
	private void only(TaskType type) {
		visible.clear();
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
			visible.removeAll(toRemove);
			selected[i] = false;
		}
	}
	
	/**
	 * Reloads, refilters and resorts all tasks.
	 */
	private void reload() {
		visible.clear();
		final TaskType[] types = TaskType.values();
		for(int i = 0; i < selected.length; i++) {
			if(selected[i]) {
				visible.addAll(filter(getTaskList(types[i])));
			}
		}
		sortVisible();
		taskListEvents.add(new TaskListEvent.FilterAll(getIds(visible)));
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
	public synchronized void updateFilter(FilterForm filter) {
		generalFilter = filter;
		reload();
		
	}
	
	/**
	 * Apply/update search filtering
	 */
	public void updateSearch(SearchForm search) {
		this.searchFilter = search;
		reload();
	}
	
	/**
	 * Apply/update task sorting settings
	 */
	public void updateSort(SortForm sort) {
		this.sort = sort;
		sortVisible();
		taskListEvents.add(new TaskListEvent.SortAll(getIds(visible)));
	}
	
	/*--------------------------------------------------
	 * Retrieve session information
	 * ---------------------------------------------------*/
	
	public List<Task> getTasks() {
		return visible.copy();
	}
	
	/**
	 * RETRIEVED EVENTS WILL BE DELETED.
	 * The same even cannot be retrieved multiple times.
	 */
	public List<TaskListEvent> retrieveEvents() {
		synchronized (taskListEvents) {
			List<TaskListEvent> result = taskListEvents.copy();
			taskListEvents.clear();
			return result;
		}
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
	
	public SearchForm getSearchForm() {
		return searchFilter;
	}
}
