package gmm.web.sessions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import gmm.collections.List;
import gmm.domain.task.Task;
import gmm.service.ajax.BundledMessageResponses;
import gmm.service.ajax.MessageResponse;
import gmm.service.ajax.NewTaskResponses;
import gmm.service.data.DataAccess;
import gmm.web.forms.TaskForm;

@Component
@Scope(value="session", proxyMode=ScopedProxyMode.TARGET_CLASS)
public class TaskSession {
	
	@Autowired private DataAccess data;
	
	/*--------------------------------------------------
	 * Add new task (conflict checking)
	 * ---------------------------------------------------*/
	
	private NewTaskResponses newTaskCheck;
	private Task newTask;
	
	public List<MessageResponse> firstTaskCheck(Task assetTask, TaskForm form) {
		this.newTask = assetTask;
		newTaskCheck = new NewTaskResponses(assetTask, form);
		return addOnSuccess(newTaskCheck.loadFirst());
	}
	
	public List<MessageResponse> getNextTaskCheck(String operation) {
		if(newTaskCheck == null) {
			throw new IllegalStateException("Call method FIRST_AssetTaskCheck first!");
		} else {
			return addOnSuccess(newTaskCheck.loadNext(operation));
		}
	}
	
	private List<MessageResponse> addOnSuccess(List<MessageResponse> responses) {
		MessageResponse last = responses.get(responses.size()-1);
		if(last.getStatus().equals(BundledMessageResponses.finished)) {
			data.add(newTask);
		}
		return responses;
	}
}
