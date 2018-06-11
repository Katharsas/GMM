package gmm.web.sessions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import gmm.collections.Collection;
import gmm.collections.HashSet;
import gmm.collections.LinkedList;
import gmm.collections.List;
import gmm.collections.Set;
import gmm.domain.UniqueObject;
import gmm.domain.User;
import gmm.domain.task.Task;
import gmm.service.data.DataAccess;
import gmm.service.data.DataChangeEvent;
import gmm.service.data.DataChangeEvent.ClientDataChangeEvent;
import gmm.service.data.DataChangeType;
import gmm.web.sessions.tasklist.StaticTaskListState;
import gmm.web.sessions.tasklist.TaskListEvent;

@Component
@Scope(value="session", proxyMode=ScopedProxyMode.TARGET_CLASS)
public class LinkSession extends StaticTaskListState {

	private final DataAccess data;
	
	private final List<Task> tasks = new LinkedList<>(Task.class);
	
	private final List<ClientDataChangeEvent> taskDataEvents;
	
	@Autowired
	public LinkSession(DataAccess data) {
		this.data = data;
		
		taskDataEvents = new LinkedList<>(ClientDataChangeEvent.class);
		data.registerForUpdates(this, Task.class);
	}
	
	/**
	 * @param ids - String of task id/ids separated by comma.
	 * @param key - linkKey of given task or key from taskToLinkKeyMapping for multiple tasks.
	 */
	public void setTaskLinks(String ids, String key) {
		final String[] idArray = ids.split(",");
		if (idArray.length < 1) throw new IllegalArgumentException("No task ID specified!");
		tasks.clear();
		final Collection<Task> allTasks = data.getList(Task.class);
		//if one, check key from task
		if(idArray.length == 1) {
			final Task task = UniqueObject.getFromId(allTasks, Long.parseLong(idArray[0]));
			if (task != null && task.getLinkKey().equals(key)) tasks.add(task);
			else throw new IllegalArgumentException("Task not found or wrong link key!");
		}
		//if multiple, check key from mapping
		else {
			final Set<Long> idSet = new HashSet<>(Long.class);
			for (final String id : idArray) {
				idSet.add(Long.parseLong(id));
			}
			final String mappedKey = data.getCombinedData().getTaskToLinkKeys().get(idSet);
			if (mappedKey != null && mappedKey.equals(key)) {
				for (final long id : idSet) {
					final Task task = UniqueObject.getFromId(allTasks, id);
					if (task != null) tasks.add(task);
				}
			}
			else throw new IllegalArgumentException("Taskgroup not found or wrong link key!");
		}
		final List<String> addedIds = getIds(tasks);
		taskListEvents.add(new TaskListEvent.AddAll(User.NULL, addedIds, addedIds));
	}
	
	public List<Task> getLinkedTasks() {
		return tasks;
	}
	
	/**
	 * RETRIEVED EVENTS WILL BE DELETED.
	 * The same even cannot be retrieved multiple times.
	 */
	public List<TaskListEvent> retrieveEvents() {
		synchronized (taskListEvents) {
			final List<TaskListEvent> result = taskListEvents.copy();
			taskListEvents.clear();
			return result;
		}
	}
	
	@Override
	protected List<Task> getVisible() {
		return tasks;
	}
	
	@Override
	protected void sortVisible() {}
	
	
	@Override
	public void onEvent(DataChangeEvent<Task> event) {
		if (!event.type.equals(DataChangeType.ADDED)) {
			taskDataEvents.add(event.toClientEvent());
		}
		super.onEvent(event);
	}
	
	public List<ClientDataChangeEvent> retrieveTaskDataEvents() {
		final List<ClientDataChangeEvent> result = taskDataEvents.copy();
		taskDataEvents.clear();
		return result;
	}
}
