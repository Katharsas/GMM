package gmm.service.assets;

import java.nio.file.Path;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import gmm.domain.task.asset.AssetGroupType;
import gmm.domain.task.asset.AssetName;
import gmm.service.data.PathConfig;
import gmm.service.tasks.AssetTaskService;

/**
 * Immutable.
 * 
 * @author Jan Mothes
 */
public class OriginalAssetFileInfo implements AssetInfo {

	@XStreamAsAttribute
	private final Path assetFile;
	
	@XStreamAsAttribute
	private final AssetName assetFileName;
	
	OriginalAssetFileInfo() {
		assetFile = null;
		assetFileName = null;
	}
	
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
	public Path getAssetFilePathAbsolute(PathConfig config) {
		return config.assetsOriginal()
				.resolve(assetFile);
	}
}
