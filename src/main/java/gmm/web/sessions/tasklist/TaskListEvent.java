package gmm.web.sessions.tasklist;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gmm.domain.User;
import gmm.domain.User.UserId;

/**
 * Events representing a change to a SINGLE (!) task list, thus syncing that list.
 * Do not mix up with task cache synching which always effects all tasklist of a session.
 * 
 * @author Jan Mothes
 */
public abstract class TaskListEvent {
	
	@SuppressWarnings("unused")
	private final static Logger logger = LoggerFactory.getLogger(TaskListEvent.class);
	
	public final UserId source;
	public final String eventName;
	
	public TaskListEvent(User source) {
		eventName = this.getClass().getSimpleName();
		this.source = source.getUserId();
	}
	
	@Override
	public String toString() {
		return "Event[name:" + eventName + "]";
	}
	
	/**
	 * A single task has changed its position inside the list.
	 * One possible result of a task having been edited (the other being removal of the task).
	 */
	public static class SortSingle extends TaskListEvent {
		public final String movedId;
		public final int newPos;
		public SortSingle(User source, String movedId, int newPos) {
			super(source);
			this.movedId = movedId;
			this.newPos = newPos;
		}
	}
	
	/**
	 * The order of the list has changed arbitrarily.
	 * Holds updated order information in form of a sorted id list.
	 */
	public static class SortAll extends TaskListEvent {
		public final List<String> visibleIdsOrdered;
		public SortAll(User source, List<String> visibleIdsOrdered) {
			super(source);
			this.visibleIdsOrdered = visibleIdsOrdered;
		}
	}
	
	/**
	 * Multiple tasks have arbitrarily been added and/or removed.
	 * Same as SortAll with the difference that the id list containing the new order may also have
	 * additional elements or may miss elements that were present before.
	 */
	public static class FilterAll extends SortAll {
		public FilterAll(User source, List<String> visibleIdsOrdered) {
			super(source, visibleIdsOrdered);
		}
	}
	
	/**
	 * A single task has been added to the list.
	 * Holds position at which the task is to be inserted into the list.
	 */
	public static class AddSingle extends TaskListEvent {
		public final String addedId;
		public final int insertedAtPos;
		public AddSingle(User source, String addedId, int insertedAtPos) {
			super(source);
			this.addedId = addedId;
			this.insertedAtPos = insertedAtPos;
		}
	}
	
	/**
	 * Multiple tasks have been added to the list in arbitrary order.
	 * Holds information about which elements exactly were added. Includes new order information
	 * similar to SortAll information.
	 */
	public static class AddAll extends SortAll {
		public final List<String> addedIds;
		public AddAll(User source, List<String> addedIds, List<String> visibleIdsOrdered) {
			super(source, visibleIdsOrdered);
			this.addedIds = addedIds;
		}
	}
	
	/**
	 * A single task has been removed from the list.
	 * Caused by task deletion or task edit (if the changed data causes it to get filtered away).
	 */
	public static class RemoveSingle extends TaskListEvent {
		public final String removedId;
		public RemoveSingle(User source, String removedId) {
			super(source);
			this.removedId = removedId;
		}
	}
	
	/**
	 * Multiple tasks have been removed from the list.
	 */
	public static class RemoveAll extends TaskListEvent {
		public final List<String> removedIds;
		public RemoveAll(User source, List<String> removedIds) {
			super(source);
			this.removedIds = removedIds;
		}
	}
}
