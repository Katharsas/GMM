package gmm.service.tasks;

import gmm.domain.GeneralTask;
import gmm.web.forms.TaskForm;
import gmm.web.sessions.TaskSession;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GeneralTaskService extends TaskService<GeneralTask> {

	@Autowired private TaskSession session;
	
	@Override
	public Class<GeneralTask> getTaskType() {
		return GeneralTask.class;
	}

	@Override
	public GeneralTask create(TaskForm form) throws IOException {
		GeneralTask task = new GeneralTask(session.getUser());
		edit(task, form);
		return task;
	}
}
