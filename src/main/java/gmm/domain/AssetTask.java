package gmm.domain;

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
	
	public A getNewestAsset() {
		return newestAsset;
	}
	
	public void setNewestAsset(A newestAsset) {
		this.newestAsset = newestAsset;
	}
}
