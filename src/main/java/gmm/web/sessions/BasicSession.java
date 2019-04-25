package gmm.web.sessions;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import gmm.collections.ArrayList;
import gmm.collections.LinkedList;
import gmm.collections.List;
import gmm.domain.task.Task;
import gmm.service.data.DataAccess;
import gmm.service.data.DataAccess.DataChangeCallback;
import gmm.service.data.DataChangeEvent;
import gmm.service.data.DataChangeEvent.ClientDataChangeEvent;
import gmm.service.data.DataChangeType;
import gmm.service.users.CurrentUser;
import gmm.web.ControllerArgs;
import gmm.web.FtlRenderer;
import gmm.web.FtlRenderer.TaskRenderResult;

@Component
@Scope(value="session", proxyMode=ScopedProxyMode.TARGET_CLASS)
public class BasicSession implements DataChangeCallback<Task> {

	private final DataAccess data;
	private final FtlRenderer ftlRenderer;
	private final CurrentUser user;
	
	// Events that affect the task cache (for all task lists/dialogs on the page) are
	// buffered here until the client retrieves them.
	private final List<ClientDataChangeEvent> taskDataEvents;
	
	@Autowired
	public BasicSession(DataAccess data, FtlRenderer ftlRenderer, CurrentUser user) {
		this.data = data;
		this.ftlRenderer = ftlRenderer;
		this.user = user;
		
		taskDataEvents = new LinkedList<>(ClientDataChangeEvent.class);
		data.registerForUpdates(this, Task.class);
	}
	
	/*--------------------------------------------------
	 * Events for task cache
	 * ---------------------------------------------------*/
	
	@PreDestroy
	private void destroy() {
		data.unregister(this);
	}
	
	/**
	 * To update existing tasks in cache, only edit and remove events are relevant.
	 */
	@Override
	public void onEvent(DataChangeEvent<? extends Task> event) {		
		if (!event.type.equals(DataChangeType.ADDED)) {
			taskDataEvents.add(event.toClientEvent());
		}
	}
	
	public List<ClientDataChangeEvent> retrieveTaskDataEvents() {
		final List<ClientDataChangeEvent> result = taskDataEvents.copy();
		taskDataEvents.clear();
		return result;
	}
	
	/*--------------------------------------------------
	 * Render tasks helper function
	 * ---------------------------------------------------*/
	
	public static class TaskDataResult {
		public String idLink;
		public Boolean isPinned;
		public TaskRenderResult render;
	}
	
	public List<TaskDataResult> renderTasks(Collection<String> idLinksToRender, Iterable<Task> dataSource, ControllerArgs args) {
		
		ArrayList<String> idLinks = new ArrayList<>(String.class, idLinksToRender);
		final List<Task> tasks = new LinkedList<>(Task.class);
		for(final Task task : dataSource) {
			final boolean contains = idLinks.remove(task.getIdLink());
			if (contains) {
				tasks.add(task);
			}
		}
		final Map<Task, TaskRenderResult> renders = ftlRenderer.renderTasks(tasks, args);
		
		final List<TaskDataResult> results = new ArrayList<>(TaskDataResult.class, idLinks.size());
		for (final Entry<Task, TaskRenderResult> entry : renders.entrySet()) {
			final TaskDataResult data = new TaskDataResult();
			final Task task = entry.getKey();
			data.idLink = task.getIdLink();
			if (user.isLoggedIn()) {
				data.isPinned = user.get().getPinnedTaskIds().contains(task.getId());
			} else {
				data.isPinned = false;
			}
			data.render = entry.getValue();
			results.add(data);
		}
		return results;
	}
}
