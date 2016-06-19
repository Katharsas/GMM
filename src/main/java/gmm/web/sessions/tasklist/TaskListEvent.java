package gmm.web.sessions.tasklist;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Edit is implemented as "RemoveSingle" followed by "AddSingle".
 */
public abstract class TaskListEvent {
	
	@SuppressWarnings("unused")
	private final static Logger logger = LoggerFactory.getLogger(TaskListEvent.class);
	
	public final String eventName;
	
	public TaskListEvent() {
		eventName = this.getClass().getSimpleName();
	}
	
	@Override
	public String toString() {
		return "Event[name:" + eventName + "]";
	}
	
	public static class SortAll extends TaskListEvent {
		public final List<String> visibleIdsOrdered;
		public SortAll(List<String> visibleIdsOrdered) {
			this.visibleIdsOrdered = visibleIdsOrdered;
		}
	}
	
	public static class FilterAll extends SortAll {
		public FilterAll(List<String> visibleIdsOrdered) {
			super(visibleIdsOrdered);
		}
	}
	
	public static class CreateAll extends SortAll {
		public final List<String> createdIds;
		public CreateAll(List<String> visibleIdsOrdered, List<String> createdIds) {
			super(visibleIdsOrdered);
			this.createdIds = createdIds;
		}
	}
	
	public static class CreateSingle extends TaskListEvent {
		public final String createdId;
		public final int insertedAtPos;
		public CreateSingle(String createdId, int insertedAtPos) {
			this.createdId = createdId;
			this.insertedAtPos = insertedAtPos;
		}
	}
	
	public static class RemoveSingle extends TaskListEvent {
		public final String removedId;
		public RemoveSingle(String removedId) {
			this.removedId = removedId;
		}
	}
	
	public static class RemoveAll extends TaskListEvent {
		public final List<String> removedIds;
		public RemoveAll(List<String> removedIds) {
			this.removedIds = removedIds;
		}
	}
}
