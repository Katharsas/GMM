package gmm.domain.task.asset;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

import gmm.collections.HashSet;
import gmm.collections.Set;
import gmm.domain.User;
import gmm.domain.task.TaskType;

public class TextureTask extends AssetTask<TextureProperties> {
	
	@XStreamOmitField
	private final Set<ModelTask> modelTasks = new HashSet<>(ModelTask.class);
	
	TextureTask() {}
	
	public TextureTask(User author, AssetName assetName) {
		super(author, assetName);
	}

	@Override
	public TaskType getType() {
		return TaskType.TEXTURE;
	}
	
	public Set<ModelTask> getModelTasks() {
		return modelTasks;
	}
}
