package gmm.web.sessions.tasklist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gmm.collections.ArrayList;
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
public abstract class TaskListState implements DataChangeCallback<Task> {
	
	@SuppressWarnings("unused")
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	protected final List<TaskListEvent> taskListEvents;
	
	public TaskListState() {
		taskListEvents = new LinkedList<>(TaskListEvent.class);
	}
	
	@Override
	public void onEvent(DataChangeEvent<Task> event) {		
		if(event.isSingleItem) {
			switch(event.type) {
			case ADDED:
				onAdd(event.source, event.getChangedSingle());
				break;
			case REMOVED:
				onRemove(event.source, event.getChangedSingle());
				break;
			case EDITED:
				onEdit(event.source, event.getChangedSingle());
				break;
			}
		} else {
			switch(event.type) {
			case ADDED:
				onAddAll(event.source, event.changed);
				break;
			case REMOVED:
				onRemoveAll(event.source, event.changed);
				break;
			case EDITED:
				onEditAll(event.source, event.changed);
				break;
			}
		}
	}
	
	public <T extends Task> void onAdd(User source, T task) {
		if(shouldBeVisible(task)) {
			synchronized (this) {
				getVisible().add(task);
				sortVisible();
				final int index = getVisible().indexOf(task);
				taskListEvents.add(new TaskListEvent.AddSingle(source, task.getIdLink(), index));
			}
		}
	}
	
	public <T extends Task> void onAddAll(User source, Collection<T> tasks) {
		final Collection<T> visibleTypeAdded = new ArrayList<>(tasks.getGenericType());
		for(final T task : tasks) {
			if (isTaskTypeVisible(TaskType.fromClass(task.getClass()))) {
				visibleTypeAdded.add(task);
			}
		}
		final Collection<T> filteredAdded = new LinkedList<>(tasks.getGenericType());
		filteredAdded.addAll(filter(visibleTypeAdded));
		if (filteredAdded.size() >= 1) {
			synchronized(this) {
				getVisible().addAll(filteredAdded);
				sortVisible();
				final List<String> visibleIdsOrdered = getIds(getVisible());
				final List<String>  addedIds = getIds(filteredAdded);
				taskListEvents.add(new TaskListEvent.AddAll(source, addedIds, visibleIdsOrdered));
			}
		}
	}
	
	public synchronized <T extends Task> void onRemove(User source, T task) {
		final boolean wasVisible = getVisible().remove(task);
		if (wasVisible) {
			taskListEvents.add(new TaskListEvent.RemoveSingle(source, task.getIdLink()));
		}
	}
	
	public synchronized <T extends Task> void onRemoveAll(User source, Collection<T> tasks) {
		getVisible().removeAll(tasks);
		taskListEvents.add(new TaskListEvent.RemoveAll(source, getIds(tasks)));
	}
	
	public <T extends Task> void onEdit(User source, T task) {
		final boolean wasVisible = isVisible(task);
		final boolean isVisible = shouldBeVisible(task);
		synchronized (this) {
			if (wasVisible) {
				getVisible().remove(task);
			}
			if(isVisible) {
				getVisible().add(task);
				sortVisible();
			}
			if (!isVisible && !wasVisible) return;
			else {
				final int index = isVisible ? getVisible().indexOf(task) : -1;
				taskListEvents.add(wasVisible ?
						new TaskListEvent.EditSingle(source, task.getIdLink(), index)
						: new TaskListEvent.AddSingle(source, task.getIdLink(), index));
			}
		}
	}
	
	public <T extends Task> void onEditAll(User source, Collection<T> tasks) {
		final Collection<T> toRemove = new LinkedList<>(tasks.getGenericType());
		final Collection<T> toAdd = new LinkedList<>(tasks.getGenericType());
		for(final T task : tasks) {
			if (isVisible(task)) {
				toRemove.add(task);
			}
			if (shouldBeVisible(task)) {
				toAdd.add(task);
			}
		}
		synchronized (this) {
			getVisible().removeAll(toRemove);
			getVisible().addAll(toAdd);
			sortVisible();
			final List<String> removedIds = getIds(toRemove);
			final List<String> addedIds = getIds(toAdd);
			final List<String> visibleIdsOrdered = getIds(getVisible());
			taskListEvents.add(new TaskListEvent.EditAll(source, removedIds, addedIds, visibleIdsOrdered));
		}
	}
	
	/**
	 * Allow client to retrieve initial TaskListState after reloading page.
	 */
	public void createInitEvent() {
		taskListEvents.clear();
		final List<String> initialIds =  getIds(getVisible());
		taskListEvents.add(new TaskListEvent.AddAll(User.NULL, initialIds, initialIds));
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
	
	
	private <T extends Task> boolean isVisible(T task) {
		return getVisible().indexOf(task) >= 0;
	}
	
	/**
	 * Checks type with {@link #isTaskTypeVisible(TaskType)} and filters with {@link #filter(Collection)}.
	 */
	private <T extends Task> boolean shouldBeVisible(T task) {
		final Class<T> type = Util.classOf(task);
		final List<T> single = new LinkedList<>(type, task);
		if(isTaskTypeVisible(TaskType.fromClass(type))) {
			final Collection<T> singleFiltered = filter(single);
			return singleFiltered.size() >= 1;
		} else {
			return false;
		}
	}
}
