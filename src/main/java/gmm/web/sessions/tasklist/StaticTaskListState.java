package gmm.web.sessions.tasklist;

import gmm.collections.Collection;
import gmm.domain.User;
import gmm.domain.task.Task;
import gmm.domain.task.TaskType;

public abstract class StaticTaskListState extends TaskListState {
	
	@Override
	public <T extends Task> void onAdd(User source, T task) {}
	@Override
	public <T extends Task> void onAddAll(User source, Collection<T> tasks) {}
	@Override
	protected boolean isTaskTypeVisible(TaskType type) {return true;}
	@Override
	protected <T extends Task> Collection<T> filter(Collection<T> tasks) {return tasks;}
}
