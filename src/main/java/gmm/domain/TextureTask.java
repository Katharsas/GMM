package gmm.domain;

import java.io.File;

import gmm.service.AssetService;
import gmm.util.HashSet;
import gmm.util.Set;

public class TextureTask extends Task {
	
	final public Set<ModelTask> models = new HashSet<ModelTask>();
	
	private String originalAssetPath = null;
	private String newAssetFolderPath = null;
	
	//not persisted (needs to be updated on load)
//	private File originalAsset;
//	private File newAssetFolder;
	
	public TextureTask(String idName, User author) {
		super(idName, author);
	}
	
	public void setAssetFolderPaths(String originalAssetPath, String newAssetFolderPath) {
		this.originalAssetPath = originalAssetPath;
		this.newAssetFolderPath = newAssetFolderPath;
	}
	
	public void updateAssetAccess(AssetService service) {
//		if(!(originalAssetPath == null)) {
//			originalAsset = new File(originalAssetPath);
//		}
//		newAssetFolder = service.linkNewAssetFolder(newAssetFolderPath);
		service.linkNewAssetFolder(newAssetFolderPath);
	}
	
	public boolean hasOriginalPreview() {
		return false;
	}
	public boolean hasNewPreview() {
		return false;
	}
	
	public String getNewAssetFolderPath() {
		return newAssetFolderPath;
	}
	
	public String getOriginalAssetPath() {
		return originalAssetPath;
	}
}
