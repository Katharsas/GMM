package gmm.domain.task.asset;

import gmm.collections.HashSet;
import gmm.collections.Set;
import gmm.domain.User;
import gmm.domain.task.TaskType;

public class TextureTask extends AssetTask<TextureProperties> {
	
	final public Set<ModelTask> models = new HashSet<>(ModelTask.class);
	
	public TextureTask(User author, AssetName assetName) {
		super(author, assetName);
	}

	@Override
	public TaskType getType() {
		return TaskType.TEXTURE;
	}
}
