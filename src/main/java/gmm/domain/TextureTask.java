package gmm.domain;

//import java.io.IOException;

//import gmm.service.AssetService;
import gmm.util.HashSet;
import gmm.util.Set;

public class TextureTask extends Task {
	
	final public Set<ModelTask> models = new HashSet<ModelTask>();
	
	private String originalAssetPath = null;
	private String newAssetFolderPath = null;
	
	private String newestAssetName = null;
	
	public TextureTask(String idName, User author) {
		super(idName, author);
	}
	
	public void setAssetFolderPaths(String originalAssetPath, String newAssetFolderPath) {
		this.originalAssetPath = originalAssetPath;
		this.newAssetFolderPath = newAssetFolderPath;
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
	public String getNewestAssetName() {
		return newestAssetName;
	}
	public void setNewestAssetName(String newestAssetName) {
		this.newestAssetName = newestAssetName;
	}
}
