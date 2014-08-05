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
			// TODO Auto-generated method stub
			return 0;
		}
		
	}),
	CREATED(new Comparator<Task>() {
		@Override
		public int compare(Task task0, Task task1) {
//			Date d0 = task0.getCreationDate();
//			Date d1 = task1.getCreationDate();
			// TODO Auto-generated method stub
			return 0;
		}
	}),
	COMMENTCOUNT(new Comparator<Task>() {
		@Override
		public int compare(Task task0, Task task1) {
			return task0.getComments().size() - task1.getComments().size();
		}
	});
			
	public final Comparator<? extends Task> comparator;
	private TaskSortAttribute(Comparator<? extends Task> comparator) {
		this.comparator = comparator;
	}
	
	//corresponding message keys
	private final String name = getTypeKey() + "." + this.name().toLowerCase();
	public String getNameKey() {return name;}
	public String getTypeKey() {return "tasks.sort";}
}
