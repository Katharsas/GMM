package gmm.service.sort;

import gmm.domain.Task;

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
	COMMENTCOUNT(new Comparator<Task>() {
		@Override
		public int compare(Task task0, Task task1) {
			return task0.getComments().size() - task1.getComments().size();
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
