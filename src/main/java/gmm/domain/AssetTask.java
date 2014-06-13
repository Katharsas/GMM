package gmm.domain;

import gmm.service.Spring;
import gmm.service.data.DataConfigService;

import java.nio.file.Path;

public class AssetTask<A extends Asset> extends Task {

	private A originalAsset = null;
	private A newestAsset = null;
	
	private Path newAssetFolder = null;
	
	//Methods--------------------------------------------
	public AssetTask(User author) {
		super(author);
	}
	
	@Override
	public void onLoad() {
		DataConfigService config = Spring.get(DataConfigService.class);
		if(originalAsset!=null) originalAsset.setBase(config.ASSETS_ORIGINAL);
		if(newestAsset!=null) newestAsset.setBase(config.ASSETS_NEW);
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
	public void setNewestAsset(A newestAsset) {
		this.newestAsset = newestAsset;
	}
	public A getNewestAsset() {
		return newestAsset;
	}
}
