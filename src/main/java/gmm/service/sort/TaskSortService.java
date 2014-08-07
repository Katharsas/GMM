package gmm.service.sort;

import java.util.Collections;
import java.util.Objects;

import org.springframework.stereotype.Service;

import gmm.collections.Collection;
import gmm.collections.LinkedList;
import gmm.collections.List;
import gmm.domain.Task;
import gmm.web.forms.SortForm;

@Service
public class TaskSortService {
	
	public <T extends Task, I extends Collection<T>> List<T> sort(I tasks, SortForm form) {
		Objects.requireNonNull(tasks);
		Objects.requireNonNull(form);
		
		LinkedList<T> list = new LinkedList<>(tasks);
		TaskSortAttribute primary = form.getSortByPrimary();
		TaskSortAttribute secondary = form.getSortBySecondary();
		Objects.requireNonNull(primary);
		
		if(form.isSortDownPrimary() && form.isSortDownSecondary()) {
			if (secondary != null) {
				Collections.sort(list, secondary.getComparator());
			}
			Collections.sort(list, primary.getComparator());
		}
		else if (!form.isSortDownPrimary() && form.isSortDownSecondary()) {
			if (secondary != null) {
				Collections.sort(list, secondary.getComparator());
				Collections.reverse(list);
			}
			Collections.sort(list, primary.getComparator());
			Collections.reverse(list);
		}
		else if(form.isSortDownPrimary() && !form.isSortDownSecondary()) {
			if (secondary != null) {
				Collections.sort(list, secondary.getComparator());
				Collections.reverse(list);
			}
			Collections.sort(list, primary.getComparator());
		}
		else {
			if (secondary != null) {
				Collections.sort(list, secondary.getComparator());
				Collections.sort(list, primary.getComparator());
			}
			Collections.reverse(list);
		}
		return list;
	}
}
