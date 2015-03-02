package gmm.domain.task;

import gmm.domain.User;


public class GeneralTask extends Task {

	public GeneralTask(User author) throws Exception {
		super(author);
	}

	@Override
	public TaskType getType() {
		return TaskType.GENERAL;
	}
}
