package gmm.service.sort;

import java.util.SortedSet;

import gmm.collections.Collection;
import gmm.collections.LinkedList;
import gmm.domain.Task;
import gmm.web.forms.SortForm;

public class TaskSortService {
	public <T extends Task, I extends Collection<T>> void sort(I tasks, SortForm form) {
		LinkedList<T> list = new LinkedList<>(tasks);
		TaskSortAttribute primary = form.getSortByPrimary();
		//TODO sort
	}
}
