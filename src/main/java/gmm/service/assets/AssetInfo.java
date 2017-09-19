package gmm.service.assets;

import java.nio.file.Path;

import gmm.domain.task.asset.AssetGroupType;
import gmm.domain.task.asset.AssetName;
import gmm.domain.task.asset.AssetProperties;
import gmm.service.data.DataConfigService;

/**
 * Represents the file (storage) information about an asset like actual name, key name, type and file path and status.
 * Allows to represent the result of basic asset file identification and validation process without inspecting the asset
 * content itself.
 * Since this is lightweight information easy to collect and allowed to change often (even when asset content itself
 * does not change), it is separate from {@link AssetProperties} and preview generation, which both are dependent on
 * inspection of the actual asset content.
 * 
 * Example:
 * Moving an asset from one folder to another may change this, but not {@link AssetProperties} or previews.
 * 
 * 
 * @author Jan Mothes
 */
public interface AssetInfo {

	public AssetName getAssetFileName();
	public AssetGroupType getType();
	public Path getDisplayPath();
	
	public Path getAssetFilePathAbsolute(DataConfigService config);
}
