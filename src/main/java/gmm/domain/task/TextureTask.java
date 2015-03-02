package gmm.domain.task;

import java.nio.file.Path;

import gmm.collections.HashSet;
import gmm.collections.Set;
import gmm.domain.User;

public class TextureTask extends AssetTask<Texture> {
	
	final public Set<ModelTask> models = new HashSet<ModelTask>();
	
	public TextureTask(User author, Path assetPath) throws Exception {
		super(author, assetPath);
	}

	@Override
	public TaskType getType() {
		return TaskType.TEXTURE;
	}
}
