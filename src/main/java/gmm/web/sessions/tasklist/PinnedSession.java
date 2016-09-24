package gmm.web.sessions.tasklist;

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
	
	//user logged into this session
	private final User user;
	
	@Autowired
	public PinnedSession(DataAccess data, UserService users) {
		user = users.getLoggedInUser();
		data.registerForUpdates(this);
	}

	@Override
	protected boolean isTaskTypeVisible(TaskType type) {
		return true;
	}

	@Override
	protected List<Task> getVisible() {
		return user.getPinnedTasks();
	}

	@Override
	protected void sortVisible() {}

	@Override
	protected <T extends Task> Collection<T> filter(Collection<T> tasks) {
		final Collection<T> filtered = new LinkedList<>(tasks.getGenericType());
		for (final T task : tasks) {
			if(user.getPinnedTasks().contains(task)) {
				filtered.add(task);
			}
		}
		return filtered;
	}
	
	/*--------------------------------------------------
	 * Update session information
	 * ---------------------------------------------------*/
	
	public void pin(Task task) {
		if (user.getPinnedTasks().contains(task)) {
			throw new IllegalArgumentException("Cannot pin a task that is already pinned!");
		} else {
			user.getPinnedTasks().add(task);
			final int index = user.getPinnedTasks().indexOf(task);
			taskListEvents.add(new TaskListEvent.AddSingle(user, task.getIdLink(), index));
		}
	}
	
	public void unpin(Task task) {
		if (!user.getPinnedTasks().contains(task)) {
			throw new IllegalArgumentException("Cannot unpin a task that has not been pinned!");
		} else {
			user.getPinnedTasks().remove(task);
			taskListEvents.add(new TaskListEvent.RemoveSingle(user, task.getIdLink()));
		}
	}

	/*--------------------------------------------------
	 * Retrieve session information
	 * ---------------------------------------------------*/
	
	public List<Task> getTasks() {
		return user.getPinnedTasks().copy();
	}
	
	/**
	 * RETRIEVED EVENTS WILL BE DELETED.
	 * The same event cannot be retrieved multiple times.
	 */
	public List<TaskListEvent> retrieveEvents() {
		synchronized (taskListEvents) {
			final List<TaskListEvent> result = taskListEvents.copy();
			taskListEvents.clear();
			return result;
		}
	}
}
