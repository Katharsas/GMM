package gmm.web.sessions.tasklist;


import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import gmm.collections.ArrayList;
import gmm.collections.Collection;
import gmm.collections.LinkedList;
import gmm.collections.List;
import gmm.domain.User;
import gmm.domain.task.Task;
import gmm.domain.task.TaskType;
import gmm.service.data.DataAccess;
import gmm.service.users.UserService;

/**
 * Pinned taskList state.
 * Pinned tasks have a fixed order determined by when the user pinned a task.
 * Pinned tasks cannot be searched or filtered.
 * 
 * @author Jan Mothes
 */
@Component
@Scope(value="session", proxyMode=ScopedProxyMode.TARGET_CLASS)
public class PinnedSession extends TaskListState {
	
	private DataAccess data;
	//user logged into this session
	private final User user;
	
	private final List<Task> pinnedTasks;
	
	@Autowired
	public PinnedSession(DataAccess data, UserService users) {
		this.data = data;
		user = users.getLoggedInUser();
		data.registerForUpdates(this, Task.class);
		
		final List<Long> pinnedIds = user.getPinnedTaskIds();
		pinnedTasks = new ArrayList<>(Task.class, pinnedIds.size() + 5);
		if (pinnedIds.size() > 0) {
			for (final Task task : data.getList(Task.class)) {
				if (pinnedIds.contains(task.getId())) {
					pinnedTasks.add(task);
				}
			}
		}
	}
	
	@PreDestroy
	private void destroy() {
		data.unregister(this);
	}

	@Override
	protected boolean isTaskTypeVisible(TaskType type) {
		return true;
	}

	@Override
	protected List<Task> getVisible() {
		return pinnedTasks;
	}

	@Override
	protected void sortVisible() {}

	@Override
	protected <T extends Task> Collection<T> filter(Collection<T> tasks) {
		final Collection<T> filtered = new LinkedList<>(tasks.getGenericType());
		for (final T task : tasks) {
			if(pinnedTasks.contains(task)) {
				filtered.add(task);
			}
		}
		return filtered;
	}
	
	/*--------------------------------------------------
	 * Update session information
	 * ---------------------------------------------------*/
	
	public synchronized void pin(Task task) {
		if (pinnedTasks.contains(task)) {
			throw new IllegalArgumentException("Cannot pin a task that is already pinned!");
		} else {
			pinnedTasks.add(task);
			user.getPinnedTaskIds().add(task.getId());
			final int index = pinnedTasks.indexOf(task);
			taskListEvents.add(new TaskListEvent.AddSingle(user, task.getIdLink(), index));
		}
	}
	
	public synchronized void unpin(Task task) {
		if (!pinnedTasks.contains(task)) {
			throw new IllegalArgumentException("Cannot unpin a task that has not been pinned!");
		} else {
			pinnedTasks.remove(task);
			user.getPinnedTaskIds().remove(task.getId());
			taskListEvents.add(new TaskListEvent.RemoveSingle(user, task.getIdLink()));
		}
	}

	/*--------------------------------------------------
	 * Retrieve session information
	 * ---------------------------------------------------*/
	
	/**
	 * RETRIEVED EVENTS WILL BE DELETED.
	 * The same event cannot be retrieved multiple times.
	 */
	public synchronized List<TaskListEvent> retrieveEvents() {
		final List<TaskListEvent> result = taskListEvents.copy();
		taskListEvents.clear();
		return result;
	}
}
