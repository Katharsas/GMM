package gmm.service;

import java.util.function.Function;

import org.springframework.stereotype.Service;

import gmm.collections.Collection;
import gmm.domain.User;
import gmm.domain.task.Task;
import gmm.domain.task.TaskPriority;
import gmm.domain.task.TaskStatus;
import gmm.domain.task.asset.AssetTask;
import gmm.service.filter.GmmSelection;
import gmm.web.forms.FilterForm;
import gmm.web.forms.SearchForm;

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
		
		final Function<T, String> getName = task -> task.getName();
		final Function<T, String> getDetails = task -> task.getDetails();
		final Function<T, String> getLabel = task -> task.getLabel();
		final Function<T, String> getAuthor = task -> task.getAuthor().getName();
		final Function<T, String> getAssigned = task -> {
			final User assigned = task.getAssigned();
			return assigned == null ? "" : assigned.getName();
		};
		final Function<T, String> getAssetPath = task -> {
			if (task instanceof AssetTask<?>) {
				final AssetTask<?> assetTask = (AssetTask<?>) task;
				return assetTask.getAssetPath().toString();
			} else {
				return "";
			}
		};
		
		I selected;
		if(search.isEasySearch()) {
			selected = new GmmSelection<T,I>(tasks, false)
				.uniteWith()
				.matchingAll(search.getEasy(),
						getName, getAuthor, getDetails, getLabel, getAssigned, getAssetPath)
				.getSelected();
		}
		else {
			selected = new GmmSelection<T,I>(tasks, true)
				.autoConvert(false)
				.intersectWith()
				.matching(getName, search.getName())
				.matching(getDetails, search.getDetails())
				.matching(getLabel, search.getLabel())
				.matching(getAuthor, search.getAuthor())
				.matching(getAssigned, search.getAssigned())
				.matching(getAssetPath, search.getPath())
				.getSelected();
		}
		return selected;
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
		
		final GmmSelection<T,I> selection = new GmmSelection<T,I>(tasks, true);
		selection.strictEqual(true);
		
		if (filterData.isCreatedByMe()) {
			selection.intersectWith().matching(task -> task.getAuthor(), currentUser);
		}
		if (filterData.isAssignedToMe()) {
			selection.intersectWith().matching(task -> task.getAssigned(), currentUser);
		}
		for(int i = 0; i<TaskPriority.values().length; i++) {
			if (!filterData.getPriority()[i]) {
				selection.remove().matching(task -> task.getPriority(), TaskPriority.values()[i]);
			}
		}
		for(int i = 0; i<TaskStatus.values().length; i++) {
			if (!filterData.getTaskStatus()[i]) {
				selection.remove().matching(task -> task.getTaskStatus(), TaskStatus.values()[i]);
			}
		}
		return selection.getSelected();
	}
}
