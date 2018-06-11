package gmm.service.assets;

import java.nio.file.Path;

import gmm.domain.task.asset.AssetGroupType;
import gmm.domain.task.asset.AssetName;
import gmm.service.data.DataConfigService;
import gmm.service.tasks.AssetTaskService;

/**
 * Immutable.
 * 
 * @author Jan Mothes
 */
public class OriginalAssetFileInfo implements AssetInfo {

	private final Path assetFile;
	private final AssetName assetFileName;
	
	public OriginalAssetFileInfo(AssetTaskService<?> service, Path relative) {
		this.assetFile = relative;
		assetFileName = new AssetName(assetFile);
	}
	
	@Override
	public AssetName getAssetFileName() {
		return assetFileName;
	}
	
	public Path getAssetFile() {
		return assetFile;
	}
	
	@Override
	public Path getDisplayPath() {
		return getAssetFile();
	}
	
	@Override
	public AssetGroupType getType() {
		return AssetGroupType.ORIGINAL;
	}

	@Override
	public Path getAssetFilePathAbsolute(DataConfigService config) {
		return config.assetsOriginal()
				.resolve(assetFile);
	}
}
