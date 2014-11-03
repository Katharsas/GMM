package gmm.domain;

import java.nio.file.Path;

import gmm.collections.LinkedList;
import gmm.collections.List;

public class ModelTask extends AssetTask<Model> {

	final private List<TextureTask> textures = new LinkedList<TextureTask>();
	final private List<ModelSite> sites = new LinkedList<ModelSite>();
	private int originalFaceCount;
	private int newFaceCount;
	
	public ModelTask(User author, Path assetPath) throws Exception {
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
	public List<ModelSite> getSites() {
		return sites;
	}
}
