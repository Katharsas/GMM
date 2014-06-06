package gmm.web.sessions;

import javax.annotation.PostConstruct;

import gmm.domain.GeneralTask;
import gmm.domain.ModelTask;
import gmm.domain.Task;
import gmm.domain.TextureTask;
import gmm.domain.User;
import gmm.service.TaskFilterService;
import gmm.service.UserService;
import gmm.service.data.DataAccess;
import gmm.util.Collection;
import gmm.util.HashSet;
import gmm.web.forms.FilterForm;
import gmm.web.forms.SearchForm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.core.context.SecurityContextHolder;
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
	
	//user logged into this session
	private User user;
	
	//task filtered by general filter (base for search)
	private Collection<Task> filteredTasks;
	
	//filteredTasks additionally filtered by search
	private Collection<Task> tasks;
	
	//current settings for general filter
	private FilterForm generalFilter;
	
	//the last tab the user selected
	private String currentTab;
	
	//triggers a data reload when set to true
	private boolean dirtyTasksFlag = false;
	
	@PostConstruct
	private void init() {
		currentTab = "general";
		user = users.get(((org.springframework.security.core.userdetails.User)
				SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername());
		generalFilter = new FilterForm();
		updateAndFilterTasks(currentTab);
	}
	
	/*--------------------------------------------------
	 * Retrieve session information
	 * ---------------------------------------------------*/
	
	public Collection<Task> getTasks() {
		if (dirtyTasksFlag) {
			updateAndFilterTasks(currentTab);
			dirtyTasksFlag = false;
		}
		return tasks.clone();
	}
	
	public String getTab() {
		return currentTab;
	}
	
	public User getUser() {
		return user;
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
	 * Apply/notify tab change
	 */
	public void updateTab(String tab) {
		if (!tab.equals(currentTab)) {
			updateAndFilterTasks(tab);
		}
		currentTab = tab;
	}
	
	/**
	 * Apply/update filtering
	 */
	public void updateFilter(FilterForm filter) {
		generalFilter = filter;
		updateAndFilterTasks(currentTab);
	}
	
	/**
	 * Apply/update search filtering
	 */
	public void updateSearch(SearchForm search) {
		tasks = filterService.search(filteredTasks, search);
	}
	
	/**
	 * Remove task from session data.
	 */
	public <T extends Task> void remove(T task) {
		filteredTasks.remove(task);
		tasks.remove(task);
	}
	
	/**
	 * Add task to session data
	 */
	public <T extends Task> void add(T task) {
		Collection<T> single = new HashSet<>();
		single.add(task);
		filteredTasks.addAll(filterService.filter(single, generalFilter, user));
		tasks = filteredTasks.clone();
	}
	
	/*--------------------------------------------------
	 * Private Helper methods
	 * ---------------------------------------------------*/
	
	@SuppressWarnings("unchecked")
	private void updateAndFilterTasks(String tab) {
		filteredTasks = (Collection<Task>) filterService.filter(getTaskList(tab), generalFilter, user);
		tasks = filteredTasks.clone();
	}
	
	private Collection<? extends Task> getTaskList (String tab) {
		switch(tab) {
		case "textures":
			return data.<TextureTask>getList(TextureTask.class);
		case "models":
			return data.<ModelTask>getList(ModelTask.class);
		case "general":
			return data.<GeneralTask>getList(GeneralTask.class);
		default:
			throw new UnsupportedOperationException("TaskController Error: Wrong tab name!");
		}
	}
}
