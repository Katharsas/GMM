package gmm.domain.task.asset;

import java.nio.file.Path;

import gmm.collections.LinkedList;
import gmm.collections.List;
import gmm.domain.User;
import gmm.domain.task.TaskType;

public class ModelTask extends AssetTask<Model> {

	final private List<TextureTask> textures = new LinkedList<>(TextureTask.class);
	
	public ModelTask(User author, Path assetPath) {
		super(author, assetPath);
	}

	public List<TextureTask> getTextures() {
		return textures;
	}

	@Override
	public TaskType getType() {
		return TaskType.MESH;
	}
}
