package gmm.domain.task;

import java.nio.file.Path;

import gmm.collections.LinkedList;
import gmm.collections.List;
import gmm.domain.User;

public class ModelTask extends AssetTask<Model> {

	final private List<TextureTask> textures = new LinkedList<>(TextureTask.class);
	private int originalFaceCount;
	private int newFaceCount;
	
	public ModelTask(User author, Path assetPath) {
		super(author, assetPath);
	}

	public int getOriginalFaceCount() {
		return originalFaceCount;
	}

	public void setOriginalFaceCount(int originalFaceCount) {
		this.originalFaceCount = originalFaceCount;
	}

	public int getNewFaceCount() {
		return newFaceCount;
	}

	public void setNewFaceCount(int newFaceCount) {
		this.newFaceCount = newFaceCount;
	}

	public List<TextureTask> getTextures() {
		return textures;
	}

	@Override
	public TaskType getType() {
		return TaskType.MODEL;
	}
}
