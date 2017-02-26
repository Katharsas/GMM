package gmm.service.assets;

import java.nio.file.Path;

import gmm.domain.task.asset.AssetGroupType;
import gmm.domain.task.asset.AssetName;
import gmm.service.data.DataConfigService;

public interface AssetInfo {

	public AssetName getAssetFileName();
	public AssetGroupType getType();
	
	public Path getAssetFilePathAbsolute(DataConfigService config);
}
