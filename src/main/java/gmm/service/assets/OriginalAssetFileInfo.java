package gmm.service.assets;

import java.nio.file.Path;

import gmm.domain.task.asset.AssetName;
import gmm.service.tasks.AssetTaskService;

public class OriginalAssetFileInfo {

	private final AssetTaskService<?> service;
	private final Path assetFile;
	private final AssetName assetFileName;
	
	public OriginalAssetFileInfo(AssetTaskService<?> service, Path relative) {
		this.service = service;
		this.assetFile = relative;
		assetFileName = new AssetName(assetFile);
	}
	
	public AssetName getAssetFileName() {
		return assetFileName;
	}
	
	public Path getAssetFile() {
		return assetFile;
	}
	
	public AssetTaskService<?> getService() {
		return service;
	}
}
