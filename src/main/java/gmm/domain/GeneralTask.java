package gmm.domain;


public class GeneralTask extends Task {

	public GeneralTask(User author) throws Exception {
		super(author);
	}

	@Override
	public TaskType getType() {
		return TaskType.GENERAL;
	}
}
