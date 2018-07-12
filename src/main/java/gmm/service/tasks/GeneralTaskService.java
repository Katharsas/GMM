package gmm.service.tasks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gmm.domain.User;
import gmm.domain.task.GeneralTask;
import gmm.domain.task.TaskType;
import gmm.service.data.DataAccess;
import gmm.web.forms.TaskForm;

@Service
public class GeneralTaskService extends TaskFormService<GeneralTask> {
	
	@Autowired
	public GeneralTaskService(DataAccess data) {
		super(data);
	}

	@Override
	public TaskType getTaskType() {
		return TaskType.GENERAL;
	}

	@Override
	public GeneralTask create(TaskForm form, User user) {
		final GeneralTask task = new GeneralTask(user);
		edit(task, form);
		return task;
	}
}
