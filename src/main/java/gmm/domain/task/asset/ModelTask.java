package gmm.domain.task.asset;

import gmm.domain.User;
import gmm.domain.task.TaskType;

public class ModelTask extends AssetTask<ModelProperties> {
	
	public ModelTask(User author, AssetName assetName) {
		super(author, assetName);
	}

	@Override
	public TaskType getType() {
		return TaskType.MESH;
	}
}
