package gmm.domain.task.asset;

import gmm.collections.LinkedList;
import gmm.collections.List;
import gmm.domain.User;
import gmm.domain.task.TaskType;

public class ModelTask extends AssetTask<ModelProperties> {

	final private List<TextureTask> textures = new LinkedList<>(TextureTask.class);
	
	public ModelTask(User author, AssetName assetName) {
		super(author, assetName);
	}

	public List<TextureTask> getTextures() {
		return textures;
	}

	@Override
	public TaskType getType() {
		return TaskType.MESH;
	}
}
