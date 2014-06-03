package gmm.domain;

import java.nio.file.Path;

public class AssetTask extends Task {

	private Path originalAsset = null;
	private Path newAssetFolder = null;
	
	private String newestAssetName = null;
	
	//Methods--------------------------------------------
	public AssetTask(String idName, User author) {
		super(idName, author);
	}
	
	//Setters, Getters---------------------------------
	public Path getNewAssetFolder() {
		return newAssetFolder;
	}
	public void setNewAssetFolder(Path newAssetFolderPath) {
		this.newAssetFolder = newAssetFolderPath;
	}
	
	public void setOriginalAsset(Path originalAssetPath) {
		this.originalAsset = originalAssetPath;
	}
	public Path getOriginalAsset() {
		return originalAsset;
	}
	
	public String getNewestAssetName() {
		return newestAssetName;
	}
	public void setNewestAssetName(String newestAssetName) {
		this.newestAssetName = newestAssetName;
	}
}
