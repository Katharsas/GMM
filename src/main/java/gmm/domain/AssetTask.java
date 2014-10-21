package gmm.domain;

import gmm.service.data.DataConfigService;

import java.nio.file.Path;

/**
 * 
 * 
 * @author Jan Mothes
 *
 * @param <A> type of asset
 */
public class AssetTask<A extends Asset> extends Task {
	
	private final Path assetPath;
	private A originalAsset = null;
	private A newestAsset = null;
	
	//Methods--------------------------------------------
	public AssetTask(User author, Path assetPath) {
		super(author);
		this.assetPath = assetPath;
	}
	
	//Setters, Getters---------------------------------
	
	public Path getAssetPath() {
		return assetPath;
	}
	
	public A getOriginalAsset() {
		return originalAsset;
	}

	public void setOriginalAsset(A originalAsset) {
		this.originalAsset = originalAsset;
	}
	
	public Path getOriginalAssetPath(DataConfigService config) {
		return config.ASSETS_ORIGINAL.resolve(getAssetPath());
	}
	
	public A getNewestAsset() {
		return newestAsset;
	}
	
	public void setNewestAsset(A newestAsset) {
		this.newestAsset = newestAsset;
	}
	
	public Path getNewestAssetPath(DataConfigService config) {
		return config.ASSETS_NEW.resolve(getAssetPath())
				.resolve(config.SUB_ASSETS).resolve(getNewestAsset().getPath());
	}
}
