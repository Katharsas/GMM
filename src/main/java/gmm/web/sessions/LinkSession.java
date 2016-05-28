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
import gmm.domain.task.Task;
import gmm.service.data.DataAccess;
import gmm.web.sessions.tasklist.StaticTaskListState;
import gmm.web.sessions.tasklist.TaskListEvent;

@Component
@Scope(value="session", proxyMode=ScopedProxyMode.TARGET_CLASS)
public class LinkSession extends StaticTaskListState {

	private final DataAccess data;
	
	private final List<Task> tasks = new LinkedList<>(Task.class);
	
	@Autowired
	public LinkSession(DataAccess data) {
		this.data = data;
		data.registerForUpdates(this);
	}
	
	/**
	 * @param ids - String of task id/ids seperated by comma.
	 * @param key - linkKey of given task or key from taskToLinkKeyMapping for multiple tasks.
	 */
	public void setTaskLinks(String ids, String key) {
		String[] idArray = ids.split(",");
		if (idArray.length < 1) throw new IllegalArgumentException("No task ID specified!");
		tasks.clear();
		Collection<Task> allTasks = data.getList(Task.class);
		//if one, check key from task
		if(idArray.length == 1) {
			Task task = UniqueObject.getFromId(allTasks, Long.parseLong(idArray[0]));
			if (task != null && task.getLinkKey().equals(key)) tasks.add(task);
			else throw new IllegalArgumentException("Task not found or wrong link key!");
		}
		//if multiple, check key from mapping
		else {
			Set<Long> idSet = new HashSet<>(Long.class);
			for (String id : idArray) {
				idSet.add(Long.parseLong(id));
			}
			String mappedKey = data.getCombinedData().getTaskToLinkKeys().get(idSet);
			if (mappedKey != null && mappedKey.equals(key)) {
				for (long id : idSet) {
					Task task = UniqueObject.getFromId(allTasks, id);
					if (task != null) tasks.add(task);
				}
			}
			else throw new IllegalArgumentException("Taskgroup not found or wrong link key!");
		}
		taskListEvents.add(new TaskListEvent.FilterAll(getIds(tasks)));
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
			List<TaskListEvent> result = taskListEvents.copy();
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
}
