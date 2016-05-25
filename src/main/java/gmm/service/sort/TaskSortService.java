package gmm.service.sort;

import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;

import org.springframework.stereotype.Service;

import gmm.collections.List;
import gmm.domain.task.Task;
import gmm.web.forms.SortForm;

/**
 * Provides methods to sort task lists.
 * 
 * @author Jan Mothes
 */
@Service
public class TaskSortService {
	
	public <T extends Task, I extends List<T>> void sort(I tasks, SortForm form) {
		Objects.requireNonNull(tasks);
		Objects.requireNonNull(form);
		
		final TaskSortAttribute primary = form.getSortByPrimary();
		final TaskSortAttribute secondary = form.getSortBySecondary();
		Objects.requireNonNull(primary);
		Objects.requireNonNull(secondary);
		
		final Comparator<Task> primaryComparator =
				primary.getComparator(form.isSortDownPrimary());
		
		final Comparator<Task> secondaryComparator = 
				secondary.getComparator(form.isSortDownSecondary());
		
		Collections.sort(tasks, primaryComparator.thenComparing(secondaryComparator));
	}
}
