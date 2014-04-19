package gmm.domain;

import gmm.util.LinkedList;
import gmm.util.List;

public class ModelTask extends FileTask {

	final private List<TextureTask> textures = new LinkedList<TextureTask>();
	final private List<ModelSite> sites = new LinkedList<ModelSite>();
	private int originalFaceCount;
	private int newFaceCount;
	
	public ModelTask(String idName, User author, MyFile oldFile) {
		super(idName, author, oldFile);
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
