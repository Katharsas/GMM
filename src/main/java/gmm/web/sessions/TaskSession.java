package gmm.web.sessions;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import gmm.collections.LinkedList;
import gmm.collections.List;
import gmm.domain.task.Task;
import gmm.domain.task.TaskType;
import gmm.domain.task.asset.AssetTask;
import gmm.service.ajax.BundledMessageResponses;
import gmm.service.ajax.MessageResponse;
import gmm.service.ajax.operations.AssetImportOperations;
import gmm.service.data.DataAccess;
import gmm.service.tasks.TaskServiceFinder;
import gmm.web.forms.TaskForm;

@Component
@Scope(value="session", proxyMode=ScopedProxyMode.TARGET_CLASS)
public class TaskSession {
	
	@Autowired private DataAccess data;
	@Autowired private TaskServiceFinder taskCreator;
	
	public void cleanUp() {
		importer = null;
	}
	
	/*--------------------------------------------------
	 * Prepare/save taskForm for task creation/edit
	 * ---------------------------------------------------*/
	
	private Task currentlyEdited = null;
	private TaskForm currentTaskForm = new TaskForm(); // never null
	
	public TaskForm getTaskForm() {
		return currentTaskForm;
	}
	
	public String getEditedIdLink() {
		return currentlyEdited == null ? null :
			currentlyEdited.getIdLink();
	}
	
	// Call to prepare creation of a new wask from empty taskForm
	public void setupTaskFormNewTask() {
		currentlyEdited = null;
		currentTaskForm = new TaskForm();
	}
	
	// Call to prepare editing of a task (and fill taskForm with that tasks data)
	public void setupTaskFormNewEdit(Task edited) {
		currentlyEdited = edited;
		currentTaskForm = taskCreator.prepareForm(currentlyEdited);
	}
	
	public void updateTaskForm(TaskForm taskForm) {
		Objects.requireNonNull(currentTaskForm);
		currentTaskForm = taskForm;
	}
	
	/*--------------------------------------------------
	 * Edit task
	 * ---------------------------------------------------*/
	
	public void executeEdit(TaskForm finalForm) {
		Objects.requireNonNull(finalForm);
		if (currentlyEdited.getType().equals(finalForm.getType())) {
			// since tasks are immutable, just edit their data
			taskCreator.edit(currentlyEdited, finalForm);
			// data relies on tasks being treated as immutable
			// => do what we would have done if tasks were actually immutable
			data.remove(currentlyEdited);
			data.add(currentlyEdited);
		} else {
			throw new IllegalArgumentException("The type of a task cannot be edited!");
		}
	}
	
	/*--------------------------------------------------
	 * Create new task (& conflict checking)
	 * ---------------------------------------------------*/
	
	private BundledMessageResponses<String> importer;
	
	public List<MessageResponse> firstTaskCheck(TaskForm form) {
		importer = null;
		final TaskType type = form.getType();
		if(type.equals(TaskType.GENERAL)) {
			// if is general task, just create and add, there can be no conflicts
			final Task task = taskCreator.create(type.toClass(), form);
			data.add(task);
			final String message = "Successfully added new task! ID: " + task.getId();
			final MessageResponse finished =
					new MessageResponse(BundledMessageResponses.finished, message);
			
			return new LinkedList<>(MessageResponse.class, finished);
		} else {
			// else check for assetpath conflicts
			@SuppressWarnings("unchecked")
			final Class<? extends AssetTask<?>> clazz = (Class<? extends AssetTask<?>>) type.toClass();
			final AssetImportOperations<?> ops =
					 new AssetImportOperations<>(form, clazz, data::add);
			 
			importer = new BundledMessageResponses<>(
					new LinkedList<>(String.class, form.getAssetPath()),
					ops, ()->{importer = null;});
			
			return importer.loadFirstBundle();
		}
	}
	
	public List<MessageResponse> getNextTaskCheck(String operation) {
		return importer.loadNextBundle(operation, false);
	}
}
