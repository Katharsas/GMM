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
	private final DataAccess data;
	
	@Autowired
	public PinnedSession(DataAccess data, UserService users) {
		user = users.getLoggedInUser();
		this.data = data;
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
		Collection<T> filtered = new LinkedList<>(tasks.getGenericType());
		for (T task : tasks) {
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
			throw new IllegalArgumentException("Cannot pin a task task is already pinned!");
		} else {
			data.edit(task);
//			user.getPinnedTasks().add(task);
//			final int index = user.getPinnedTasks().indexOf(task);
//			taskListEvents.add(new TaskListEvent.CreateSingle(task.getIdLink(), index));
		}
	}
	
	public void unpin(Task task) {
		if (!user.getPinnedTasks().contains(task)) {
			throw new IllegalArgumentException("Cannot pin a task task is already pinned!");
		} else {
			data.edit(task);
//			user.getPinnedTasks().remove(task);
//			taskListEvents.add(new TaskListEvent.RemoveSingle(task.getIdLink()));
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
