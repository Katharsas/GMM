package gmm.web.sessions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import gmm.collections.LinkedList;
import gmm.collections.List;
import gmm.domain.task.Task;
import gmm.domain.task.TaskType;
import gmm.domain.task.asset.Asset;
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
	 * Add new task
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
			final Class<? extends AssetTask<?>> clazz =
					(Class<? extends AssetTask<?>>) type.toClass();
			 final AssetImportOperations<? extends Asset, ? extends AssetTask<?>> ops =
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
