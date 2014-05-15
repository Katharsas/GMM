package gmm.domain;


public class AssetTask extends Task {

	private String originalAssetPath = null;
	private String newAssetFolderPath = null;
	
	private String newestAssetName = null;
	
	//Methods--------------------------------------------
	public AssetTask(String idName, User author) {
		super(idName, author);
	}
	
	//Setters, Getters---------------------------------
	public String getNewAssetFolderPath() {
		return newAssetFolderPath;
	}
	
	public String getOriginalAssetPath() {
		return originalAssetPath;
	}
	
	public String getNewestAssetName() {
		return newestAssetName;
	}
	
	public void setNewestAssetName(String newestAssetName) {
		this.newestAssetName = newestAssetName;
	}
	
	public void setAssetFolderPaths(String originalAssetPath, String newAssetFolderPath) {
		this.originalAssetPath = originalAssetPath;
		this.newAssetFolderPath = newAssetFolderPath;
	}
}
