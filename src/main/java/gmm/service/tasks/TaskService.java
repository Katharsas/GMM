package gmm.service.tasks;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;

import gmm.domain.Task;
import gmm.domain.User;
import gmm.service.UserService;
import gmm.web.forms.TaskForm;

/**
 * This class and implementing classes should be treated as package private.
 * The class {@link TaskServiceFinder} provides all methods of this interface.
 */
abstract class TaskService<T extends Task> {

	@Autowired private UserService users;
	
	public abstract Class<T> getTaskType();
	public abstract T create(TaskForm form) throws IOException ;
	
	public void edit(T task, TaskForm form) {
		task.setName(form.getName());
		task.setPriority(form.getPriority());
		task.setTaskStatus(form.getStatus());
		task.setDetails(form.getDetails());
		task.setLabel(form.getLabel());
		User assigned = form.getAssigned().equals("") ? null : users.get(form.getAssigned());
		task.setAssigned(assigned);
	}
	
	public TaskForm prepareForm(T task) {
		TaskForm form = new TaskForm();
		form.setName(task.getName());
		form.setDetails(task.getDetails());
		form.setLabel(task.getLabel());
		form.setPriority(task.getPriority());
		form.setStatus(task.getTaskStatus());
		form.setAssigned(task.getAssigned());
		return form;
	}
}
