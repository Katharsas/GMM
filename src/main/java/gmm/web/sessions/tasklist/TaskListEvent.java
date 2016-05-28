package gmm.web.sessions.tasklist;

import java.util.List;

/**
 * Edit is implemented as "RemoveSingle" followed by "AddSingle".
 */
public abstract class TaskListEvent {
	
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
