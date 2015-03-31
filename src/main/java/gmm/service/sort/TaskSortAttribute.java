package gmm.service.sort;

import gmm.domain.User;
import gmm.domain.task.Task;
import gmm.domain.task.TaskType;

import java.util.Comparator;

/**
 * Represents all attributes which can be compared for task sorting.
 * Every attribute must specify a Comparator for tasks.
 * 
 * @author Jan
 */
public enum TaskSortAttribute {
	
	ID(new Comparator<Task>() {
		@Override
		public int compare(Task task0, Task task1) {
			long result = task0.getId() - task1.getId();
			return result < 0 ? -1 : result > 0 ? 1 : 0;
		}
		
	}),
	TITLE(new Comparator<Task>() {
		@Override
		public int compare(Task task0, Task task1) {
			return task0.getName().compareTo(task1.getName());
		}
		
	}),
	CREATED(new Comparator<Task>() {
		@Override
		public int compare(Task task0, Task task1) {
			return task0.getCreationDate().compareTo(task1.getCreationDate());
		}
	}),
	AUTHOR(new Comparator<Task>() {
		@Override
		public int compare(Task task0, Task task1) {
			return task0.getAuthor().getName().compareTo(task1.getAuthor().getName());
		}
	}),
	ASSIGNED(new Comparator<Task>() {
		@Override
		public int compare(Task task0, Task task1) {
			User u1 = task0.getAssigned();
			User u2 = task1.getAssigned();
			if (u1 == null && u2 == null) return 0;
			if (u1 == null) return 1;
			if (u2 == null) return -1;
			return u1.getName().compareTo(u2.getName());
		}
	}),
	PRIORITY(new Comparator<Task>() {
		@Override
		public int compare(Task task0, Task task1) {
			return Integer.compare(task0.getPriority().ordinal(), task1.getPriority().ordinal());
		}
	}),
	STATUS(new Comparator<Task>() {
		@Override
		public int compare(Task task0, Task task1) {
			return Integer.compare(task0.getTaskStatus().ordinal(), task1.getTaskStatus().ordinal());
		}
	}),
	TYPE(new Comparator<Task>() {
		@Override
		public int compare(Task task0, Task task1) {
			TaskType t1 = TaskType.fromClass(task0.getClass());
			TaskType t2 = TaskType.fromClass(task1.getClass());
			return Integer.compare(t1.ordinal(), t2.ordinal());
		}
	}),
	COMMENTCOUNT(new Comparator<Task>() {
		@Override
		public int compare(Task task0, Task task1) {
			return task1.getComments().size() - task0.getComments().size();
		}
	});
			
	private final Comparator<? super Task> comparator;
	private TaskSortAttribute(Comparator<? super Task> comparator) {
		this.comparator = comparator;
	}
	public Comparator<? super Task> getComparator() {
		return comparator;
	}
	
	//corresponding message keys
	private final String name = getTypeKey() + "." + this.name().toLowerCase();
	public String getNameKey() {return name;}
	public String getTypeKey() {return "tasks.sort";}
}
