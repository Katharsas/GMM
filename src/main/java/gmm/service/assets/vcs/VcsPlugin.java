package gmm.service.assets.vcs;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;

import gmm.collections.List;
import gmm.service.assets.AssetService;

public abstract class VcsPlugin {

	private final AssetService assetService;
	
	@Autowired
	public VcsPlugin(AssetService assetService) {
		this.assetService = assetService;
	}
	
	/**
	 * False indicates that new assets should be saved on same path as original ones, if existent.
	 * True indicates that new assets may have a different folder structure and user must specify
	 * the path for a new asset he uploads, even if an original exists for that asset.
	 */
	public abstract boolean allowCustomAssetPaths();
	
	public void notifyFilesChanged(List<Path> changedPaths) {
		assetService.onNewAssetFilesChanged(changedPaths);
	}
}
