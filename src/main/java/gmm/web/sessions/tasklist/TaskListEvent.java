package gmm.web.sessions.tasklist;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gmm.domain.User;
import gmm.domain.User.UserId;

/**
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
	
	public static class SortAll extends TaskListEvent {
		public final List<String> visibleIdsOrdered;
		public SortAll(User source, List<String> visibleIdsOrdered) {
			super(source);
			this.visibleIdsOrdered = visibleIdsOrdered;
		}
	}
	
	public static class FilterAll extends SortAll {
		public FilterAll(User source, List<String> visibleIdsOrdered) {
			super(source, visibleIdsOrdered);
		}
	}
	
	public static class CreateAll extends SortAll {
		public final List<String> createdIds;
		public CreateAll(User source, List<String> visibleIdsOrdered, List<String> createdIds) {
			super(source, visibleIdsOrdered);
			this.createdIds = createdIds;
		}
	}
	
	public static class CreateSingle extends TaskListEvent {
		public final String createdId;
		public final int insertedAtPos;
		public CreateSingle(User source, String createdId, int insertedAtPos) {
			super(source);
			this.createdId = createdId;
			this.insertedAtPos = insertedAtPos;
		}
	}
	
	public static class RemoveSingle extends TaskListEvent {
		public final String removedId;
		public RemoveSingle(User source, String removedId) {
			super(source);
			this.removedId = removedId;
		}
	}
	
	public static class RemoveAll extends TaskListEvent {
		public final List<String> removedIds;
		public RemoveAll(User source, List<String> removedIds) {
			super(source);
			this.removedIds = removedIds;
		}
	}
	
	public static class EditSingle extends TaskListEvent {
		public final String editedId;
		public final boolean isVisible;
		public final int newPos;
		public EditSingle(User source, String editedId, int newPos) {
			super(source);
			this.editedId = editedId;
			this.newPos = newPos;
			this.isVisible = true;
		}
		public EditSingle(User source, String editedId) {
			super(source);
			this.editedId = editedId;
			this.newPos = -1;
			this.isVisible = false;
		}
	}
}
