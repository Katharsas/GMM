package gmm.service.tasks;

import gmm.domain.User;
import gmm.domain.task.Task;
import gmm.domain.task.TaskType;
import gmm.service.data.DataAccess;
import gmm.service.users.UserProvider;
import gmm.web.forms.TaskForm;

/**
 * This class and implementing classes should be treated as package private.
 * The class {@link TaskServiceFinder} provides all methods of this interface.
 */
abstract class TaskFormService<T extends Task> {
	
	private final DataAccess data;
	
	public TaskFormService(DataAccess data) {
		this.data = data;
	}
	
	public abstract TaskType getTaskType();
	public abstract T create(TaskForm form, User user);
	
	public void edit(T task, TaskForm form) {
		task.setName(form.getName());
		task.setPriority(form.getPriority());
		task.setTaskStatus(form.getStatus());
		task.setDetails(form.getDetails());
		task.setLabel(form.getLabel());
		final User assigned = form.getAssigned().equals("") ?
				null : UserProvider.get(form.getAssigned(), data);
		task.setAssigned(assigned);
	}
	
	public TaskForm prepareForm(T task) {
		final TaskForm form = new TaskForm();
		form.setName(task.getName());
		form.setDetails(task.getDetails());
		form.setLabel(task.getLabel());
		form.setPriority(task.getPriority());
		form.setStatus(task.getTaskStatus());
		form.setAssigned(task.getAssigned());
		form.setType(task.getType());
		return form;
	}
}
