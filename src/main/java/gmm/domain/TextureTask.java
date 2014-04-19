package gmm.domain;

import java.io.File;

import gmm.util.LinkedList;
import gmm.util.List;

public class TextureTask extends Task {

	final private List<ModelTask> models = new LinkedList<ModelTask>();
	private File originalAsset;
	private File newAssetFolder;
	
	public TextureTask(String idName, User author) {
		super(idName, author);
	}
	
	public void registerAsset(String originalAssetPath, String newAssetFolderPath) {
		if(!(originalAssetPath == null)) {
			this.originalAsset = new File(originalAssetPath);
		}
		this.newAssetFolder = new File(newAssetFolderPath);
	}
	
	public List<ModelTask> getModels() {
		return models;
	}
}
