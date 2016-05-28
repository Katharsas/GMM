package gmm.web.sessions.tasklist;

import com.google.common.collect.Multimap;

import gmm.collections.Collection;
import gmm.collections.LinkedList;
import gmm.collections.List;
import gmm.domain.task.Task;
import gmm.domain.task.TaskType;
import gmm.service.data.DataAccess.TaskUpdateCallback;
import gmm.util.Util;

public abstract class TaskListState implements TaskUpdateCallback {
	
	public TaskListState() {
		taskListEvents = new LinkedList<>(TaskListEvent.class);
	}
	
	final List<TaskListEvent> taskListEvents;
	
	@Override
	public <T extends Task> void onAdd(T task) {
		Class<T> type = Util.classOf(task);
		final List<T> single = new LinkedList<T>(type, task);
		if(isTaskTypeVisible(TaskType.fromClass(type))) {
			Collection<T> singleFiltered = filter(single);
			if(singleFiltered.size() >= 1) {
				synchronized (this) {
					getVisible().add(task);
					sortVisible();
					int index = getVisible().indexOf(task);
					taskListEvents.add(new TaskListEvent.CreateSingle(task.getIdLink(), index));				
				}
			}
		}
	}
	
	@Override
	public <T extends Task> void onAddAll(Collection<T> tasks) {
		Multimap<Class<? extends T>, T> typeToTask =
				LinkedList.getMultiMap(tasks.getGenericType());
		for(T task : tasks) {
			typeToTask.put(Util.getClass(task), task);
		}
		Collection<T> filteredAdded = new LinkedList<>(tasks.getGenericType());
		for(Class<? extends T> clazz : typeToTask.keys()) {
			if(isTaskTypeVisible(TaskType.fromClass(clazz))) {
				filteredAdded.addAll(filter((Collection<T>) typeToTask.get(clazz)));
			}
		}
		if (filteredAdded.size() >= 1) {
			synchronized(this) {
				getVisible().addAll(filteredAdded);
				sortVisible();
				final List<String> visibleIdsOrdered = getIds(getVisible());
				final List<String>  addedIds = getIds(filteredAdded);
				taskListEvents.add(new TaskListEvent.CreateAll(visibleIdsOrdered, addedIds));
			}
		}
	}
	
	@Override
	public synchronized <T extends Task> void onRemove(T task) {
		getVisible().remove(task);
		taskListEvents.add(new TaskListEvent.RemoveSingle(task.getIdLink()));
	}
	
	@Override
	public synchronized <T extends Task> void onRemoveAll(Collection<T> tasks) {
		getVisible().removeAll(tasks);
		taskListEvents.add(new TaskListEvent.RemoveAll(getIds(tasks)));
	}
	
	List<String> getIds(Collection<? extends Task> tasks) {
		final List<String> ids = new LinkedList<>(String.class);
		for(Task task : tasks) {
			ids.add(task.getIdLink());
		}
		return ids;
	}
	
	abstract boolean isTaskTypeVisible(TaskType type);
	abstract List<Task> getVisible(); 
	abstract void sortVisible();
	abstract <T extends Task> Collection<T> filter(Collection<T> tasks);
}
