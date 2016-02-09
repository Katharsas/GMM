package gmm.service.tasks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gmm.domain.task.GeneralTask;
import gmm.web.forms.TaskForm;
import gmm.web.sessions.TaskSession;

@Service
public class GeneralTaskService extends TaskFormService<GeneralTask> {

	@Autowired private TaskSession session;
	
	@Override
	public Class<GeneralTask> getTaskType() {
		return GeneralTask.class;
	}

	@Override
	public GeneralTask create(TaskForm form) {
		final GeneralTask task = new GeneralTask(session.getUser());
		edit(task, form);
		return task;
	}
}
