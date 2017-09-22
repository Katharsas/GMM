package gmm.web.sessions;

import java.util.Objects;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import gmm.collections.LinkedList;
import gmm.collections.List;
import gmm.domain.User;
import gmm.domain.task.Task;
import gmm.domain.task.TaskType;
import gmm.domain.task.asset.AssetName;
import gmm.service.ajax.BundledMessageResponses;
import gmm.service.ajax.ConflictAnswer;
import gmm.service.ajax.MessageResponse;
import gmm.service.ajax.operations.AssetNameConflictCheckerFactory;
import gmm.service.ajax.operations.AssetNameConflictCheckerFactory.AssetNameConflictChecker;
import gmm.service.data.DataAccess;
import gmm.service.data.DataAccess.DataChangeCallback;
import gmm.service.data.DataChangeEvent;
import gmm.service.data.DataChangeEvent.ClientDataChangeEvent;
import gmm.service.data.DataChangeType;
import gmm.service.tasks.TaskServiceFinder;
import gmm.service.users.UserService;
import gmm.web.forms.TaskForm;

@Component
@Scope(value="session", proxyMode=ScopedProxyMode.TARGET_CLASS)
public class TaskSession implements DataChangeCallback {
	
	private final DataAccess data;
	private final TaskServiceFinder taskCreator;
	private final User loggedInUser;
	private final AssetNameConflictCheckerFactory assetNameConflictCheckerFactory;
	
	/**
	 * Events that affect the task cache and all task lists on the page.
	 * On removal, task data must be removed from cache and list if visible.
	 * On edit, task data must be updated and task if visible must be be reinserted.
	 */
	private final List<ClientDataChangeEvent> taskDataEvents;
	
	@Autowired
	public TaskSession(DataAccess data, TaskServiceFinder taskCreator, UserService users,
			AssetNameConflictCheckerFactory conflictCheckerFactory) {
		this.data = data;
		this.taskCreator = taskCreator;
		loggedInUser = users.getLoggedInUser();
		this.assetNameConflictCheckerFactory = conflictCheckerFactory;
		
		taskDataEvents = new LinkedList<>(ClientDataChangeEvent.class);
		data.registerForUpdates(this);
	}
	
	public void cleanUp() {
		importer = null;
	}
	
	@Override
	public void onEvent(DataChangeEvent event) {		
		final Class<?> clazz = event.changed.getGenericType();
		if (Task.class.isAssignableFrom(clazz)) {
			if (!event.type.equals(DataChangeType.ADDED)) {
				taskDataEvents.add(event.toClientEvent());
			}
		}
	}
	
	public List<ClientDataChangeEvent> retrieveTaskDataEvents() {
		final List<ClientDataChangeEvent> result = taskDataEvents.copy();
		taskDataEvents.clear();
		return result;
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
	
	// Call to prepare creation of a new task from empty taskForm
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
			// TODO make tasks immutable
			// data relies on tasks being treated as immutable
			// => do what we would have done if tasks were actually immutable
			data.edit(currentlyEdited);
		} else {
			throw new IllegalArgumentException("The type of a task cannot be edited!");
		}
	}
	
	/*--------------------------------------------------
	 * Create new task (& conflict checking)
	 * ---------------------------------------------------*/
	
	private BundledMessageResponses<AssetName> importer;
	
	public List<MessageResponse> firstTaskCheck(TaskForm form) {
		importer = null;
		final TaskType type = form.getType();
		if(type.equals(TaskType.GENERAL)) {
			// if is general task, just create and add, there can be no conflicts
			final Task task = taskCreator.create(form, loggedInUser);
			data.add(task);
			final String message = "Successfully added new task! ID: " + task.getId();
			final MessageResponse finished =
					new MessageResponse(BundledMessageResponses.finished, message);
			
			return new LinkedList<>(MessageResponse.class, finished);
		} else {
			// else check for asset filename conflicts
			final Consumer<AssetName> onAssetNameChecked = (assetName) -> {
				form.setAssetName(assetName.get());
				data.add(taskCreator.create(form, loggedInUser));
			};
			final AssetNameConflictChecker ops = 
					assetNameConflictCheckerFactory.create(onAssetNameChecked);
			 
			importer = new BundledMessageResponses<>(
					new LinkedList<>(AssetName.class, new AssetName(form.getAssetName())),
					ops, ()->{importer = null;});
			
			return importer.firstBundle();
		}
	}
	
	public List<MessageResponse> getNextTaskCheck(ConflictAnswer answer) {
		return importer.nextBundle(answer);
	}
}
