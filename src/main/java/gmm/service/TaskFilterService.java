package gmm.service;

import gmm.collections.Collection;
import gmm.domain.Task;
import gmm.domain.TaskPriority;
import gmm.domain.TaskStatus;
import gmm.domain.User;
import gmm.service.filter.Selection;
import gmm.service.filter.SimpleSelection;
import gmm.web.forms.FilterForm;
import gmm.web.forms.SearchForm;

import org.springframework.stereotype.Service;

/**
 * Service for searching and filtering tasks.
 * @author Jan Mothes
 */
@Service
public class TaskFilterService {
	
	/**
	 * Applies the general search data to a list of GeneralTasks;
	 * @param tasks - the tasks that will be searched
	 * @param search - the search data object
	 * @return the result of the applied search
	 */
	public synchronized <T extends Task, I extends Collection<T>> I search(
			I tasks, SearchForm search) {
		
		Selection<T,I> selection;
		if(search.isEasySearch()) {
			selection = new SimpleSelection<T,I>(tasks, false)
					.bufferFilter(search.getEasy())
					.uniteWith()
					.matchingGetter("getName")
					.matchingGetter("getAuthor")
					.matchingGetter("getDetails")
					.matchingGetter("getLabel")
					.matchingGetter("getAssigned");
		}
		else {
			selection = new SimpleSelection<T,I>(tasks, true)
					.intersectWith()
					.matching("getName", search.getName())
					.matching("getAuthor", search.getAuthor())
					.matching("getDetails", search.getDetails())
					.matching("getLabel", search.getLabel())
					.matching("getAssigned", search.getAssigned());
		}
		return selection.getSelected();
	}
	
	/**
	 * Applies the general filter data to a collection of GeneralFilter
	 * @param tasks - the tasks that will be searched
	 * @param filterData - the filter data object
	 * @param currentUser - the currently logged in user
	 * @return the result of the applied filter
	 */
	public synchronized <T extends Task, I extends Collection<T>> I filter(
			I tasks, FilterForm filterData, User currentUser) {
		
		Selection<T,I> selection = new SimpleSelection<T,I>(tasks, true);
		selection.setOnlyMatchEqual(true);
		
		if (filterData.isCreatedByMe()) {
			selection.intersectWith().matching("getAuthor", currentUser);
		}
		if (filterData.isAssignedToMe()) {
			selection.intersectWith().matching("getAssigned", currentUser);
		}
		for(int i = 0; i<TaskPriority.values().length; i++) {
			if (!filterData.getPriority()[i]) {
				selection.remove().matching("getPriority", TaskPriority.values()[i]);
			}
		}
		for(int i = 0; i<TaskStatus.values().length; i++) {
			if (!filterData.getTaskStatus()[i]) {
				selection.remove().matching("getTaskStatus", TaskStatus.values()[i]);
			}
		}
		return selection.getSelected();
	}
}
