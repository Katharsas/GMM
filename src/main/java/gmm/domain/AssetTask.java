package gmm.domain;

import java.nio.file.Path;

public class AssetTask<A extends Asset> extends Task {

	private A originalAsset = null;
	private Path newAssetFolder = null;
	
	private String newestAssetName = null;
	
	//Methods--------------------------------------------
	public AssetTask(User author) {
		super(author);
	}
	
	//Setters, Getters---------------------------------
	public Path getNewAssetFolder() {
		return newAssetFolder;
	}
	public void setNewAssetFolder(Path newAssetFolderPath) {
		this.newAssetFolder = newAssetFolderPath;
	}
	
	public void setOriginalAsset(A originalAssetPath) {
		this.originalAsset = originalAssetPath;
	}
	public A getOriginalAsset() {
		return originalAsset;
	}
	
	public String getNewestAssetName() {
		return newestAssetName;
	}
	public void setNewestAssetName(String newestAssetName) {
		this.newestAssetName = newestAssetName;
	}
}
