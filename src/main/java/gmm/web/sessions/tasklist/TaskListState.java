package gmm.web.sessions.tasklist;

import com.google.common.collect.Multimap;

import gmm.collections.Collection;
import gmm.collections.LinkedList;
import gmm.collections.List;
import gmm.domain.User;
import gmm.domain.task.Task;
import gmm.domain.task.TaskType;
import gmm.service.data.DataAccess.DataChangeCallback;
import gmm.service.data.DataChangeEvent;
import gmm.util.Util;

/**
 * Buffer for events that need to be synced with a client task list.
 * 
 * @author Jan Mothes
 */
public abstract class TaskListState implements DataChangeCallback {
	
	public TaskListState() {
		taskListEvents = new LinkedList<>(TaskListEvent.class);
	}
	
	protected final List<TaskListEvent> taskListEvents;
	
	@Override
	public void onEvent(DataChangeEvent event) {
		Class<?> clazz = event.changed.getGenericType();
		if (Task.class.isAssignableFrom(clazz)) {
			Class<Task> target = Task.class;
			if(event.isSingleItem) {
				switch(event.type) {
				case ADDED:
					onAdd(event.source, event.getChangedSingle(target));
					break;
				case REMOVED:
					onRemove(event.source, event.getChangedSingle(target));
					break;
				case EDITED:
					onEdit(event.source, event.getChangedSingle(target));
					break;
				}
			} else {
				switch(event.type) {
				case ADDED:
					onAddAll(event.source, event.getChanged(target));
					break;
				case REMOVED:
					onRemoveAll(event.source, event.getChanged(target));
					break;
				case EDITED:
					for(Task task : event.getChanged(target)) {
						onEdit(event.source, task);
					}
					break;
				}
			}
		}
	}
	
	public <T extends Task> void onAdd(User source, T task) {
		final boolean isVisible = isSingleVisible(task);
		synchronized (this) {
			removeAdd(task, isVisible);
			if(isVisible) {
				sortVisible();
				final int index = getVisible().indexOf(task);
				taskListEvents.add(new TaskListEvent.CreateSingle(source, task.getIdLink(), index));
			}
		}
	}
	
	public <T extends Task> void onAddAll(User source, Collection<T> tasks) {
		final Multimap<Class<? extends T>, T> typeToTask =
				LinkedList.getMultiMap(tasks.getGenericType());
		for(final T task : tasks) {
			typeToTask.put(Util.getClass(task), task);
		}
		final Collection<T> filteredAdded = new LinkedList<>(tasks.getGenericType());
		for(final Class<? extends T> clazz : typeToTask.keys()) {
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
				taskListEvents.add(new TaskListEvent.CreateAll(source, visibleIdsOrdered, addedIds));
			}
		}
	}
	
	public synchronized <T extends Task> void onRemove(User source, T task) {
		getVisible().remove(task);
		taskListEvents.add(new TaskListEvent.RemoveSingle(source, task.getIdLink()));
	}
	
	public synchronized <T extends Task> void onRemoveAll(User source, Collection<T> tasks) {
		getVisible().removeAll(tasks);
		taskListEvents.add(new TaskListEvent.RemoveAll(source, getIds(tasks)));
	}
	
	public <T extends Task> void onEdit(User source, T task) {
		final boolean isVisible = isSingleVisible(task);
		synchronized (this) {
			removeAdd(task, isVisible);
			if(isVisible) {
				sortVisible();
				final int index = getVisible().indexOf(task);
				taskListEvents.add(new TaskListEvent.EditSingle(source, task.getIdLink(), index));
			} else {
				taskListEvents.add(new TaskListEvent.EditSingle(source, task.getIdLink()));
			}
		}
	}
	
	protected List<String> getIds(Collection<? extends Task> tasks) {
		final List<String> ids = new LinkedList<>(String.class);
		for(final Task task : tasks) {
			ids.add(task.getIdLink());
		}
		return ids;
	}
	
	protected abstract boolean isTaskTypeVisible(TaskType type);
	protected abstract List<Task> getVisible(); 
	protected abstract void sortVisible();
	protected abstract <T extends Task> Collection<T> filter(Collection<T> tasks);
	
	/**
	 * Remove task from visible (if it was visible before) and re-add it (at same position if it
	 * was visible before).
	 * @param isVisible - Only add task if this is true
	 */
	private <T extends Task> void removeAdd(T task, boolean isVisible) {
		final int oldPos = getVisible().indexOf(task);
		final boolean wasVisible = oldPos >= 0;
		if (wasVisible) {
			getVisible().remove(task);
		}
		if (isVisible) {
			if (wasVisible) {
				getVisible().add(oldPos, task);
			} else {
				getVisible().add(task);
			}
		}
	}
	
	/**
	 * Find out if a task that was added or edited should be visible or not.
	 * Checks type with {@link #isTaskTypeVisible(TaskType)} and filters with {@link #filter(Collection)}.
	 */
	private <T extends Task> boolean isSingleVisible(T task) {
		final Class<T> type = Util.classOf(task);
		final List<T> single = new LinkedList<T>(type, task);
		if(isTaskTypeVisible(TaskType.fromClass(type))) {
			final Collection<T> singleFiltered = filter(single);
			return singleFiltered.size() >= 1;
		} else {
			return false;
		}
	}
}
