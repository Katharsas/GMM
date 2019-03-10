package gmm.domain.task.asset;

import java.time.Instant;
import java.util.Objects;

import gmm.domain.User;
import gmm.domain.task.Task;
import gmm.service.assets.AssetInfo;
import gmm.service.assets.NewAssetFolderInfo;
import gmm.service.assets.NewAssetFolderInfo.AssetFolderStatus;
import gmm.service.assets.OriginalAssetFileInfo;

/**
 * Each asset task backs a specific asset, in two versions (original and new).
 * Contains additional information about those asset files and their (usually type specific) content / properties
 * (see {@link AssetProperties}).
 * 
 * @author Jan Mothes
 *
 * @param <A> type of asset
 */
public abstract class AssetTask<A extends AssetProperties> extends Task {
	
	private final AssetName assetName;
	
	private A originalAssetProps = null;
	private A newestAssetProps = null;
	
	private OriginalAssetFileInfo originalAssetFileInfo = null;
	private NewAssetFolderInfo newAssetFolderInfo = null;
	
	//used for caching of newest preview
	private Instant newestAssetLastUpdate = null;
	
	//Methods--------------------------------------------
	AssetTask() {
		assetName = null;
	}
	
	public AssetTask(User author, AssetName assetName) {
		super(author);
		this.assetName = assetName;
	}
	
	@SuppressWarnings("unchecked")
	public static Class<AssetTask<?>> getGenericClass() {
		return (Class<AssetTask<?>>) (Class<?>) AssetTask.class;
	}
	
	public AssetName getAssetName() {
		return assetName;
	}
	
	public A getAssetProperties(AssetGroupType type) {
		return type.isOriginal() ? getOriginalAssetProperties() : getNewAssetProperties();
	}
	
	public AssetInfo getAssetStorageInfo(AssetGroupType type) {
		return type.isOriginal() ? getOriginalAssetFileInfo() : getNewAssetFolderInfo();
	}
	
	public void setOriginalAsset(A assetProps, OriginalAssetFileInfo originalFileInfo) {
		if (assetProps != null || originalFileInfo != null) {
			Objects.requireNonNull(assetProps);
			Objects.requireNonNull(originalFileInfo);
			if (!assetName.equals(originalFileInfo.getAssetFileName()))
				throw new IllegalArgumentException("AssetName mismatch!");
		}
		this.originalAssetProps = assetProps;
		this.originalAssetFileInfo = originalFileInfo;
	}
	
	public void setOriginalAssetFileInfo(OriginalAssetFileInfo originalFileInfo) {
		Objects.requireNonNull(originalAssetProps);
		Objects.requireNonNull(originalFileInfo);
		if (!assetName.equals(originalFileInfo.getAssetFileName()))
			throw new IllegalArgumentException("AssetName mismatch!");
		this.originalAssetFileInfo = originalFileInfo;
	}
	
	public A getOriginalAssetProperties() {
		return originalAssetProps;
	}
	
	public OriginalAssetFileInfo getOriginalAssetFileInfo() {
		return originalAssetFileInfo;
	}

	public void setNewAsset(A assetProperties, NewAssetFolderInfo newFolderInfo) {
		setNewAssetFolderInfo(assetProperties, newFolderInfo);
		this.newestAssetLastUpdate = Instant.now();
	}
	
	public void setNewAssetFolderInfo(NewAssetFolderInfo newFolderInfo) {
		setNewAsset(newestAssetProps, newFolderInfo);
	}
	
	private void setNewAssetFolderInfo(A assetProps, NewAssetFolderInfo newFolderInfo) {
		if (newFolderInfo != null) {
			if (!assetName.equals(newFolderInfo.getAssetFolderName()))
				throw new IllegalArgumentException("AssetName mismatch!");
		}
		if(assetProps != null) {
			Objects.requireNonNull(newFolderInfo);
			if (newFolderInfo.getStatus() != AssetFolderStatus.VALID_WITH_ASSET) {
				throw new IllegalArgumentException("Asset folder status must be VALID_WITH_ASSET because properties are non-null!");
			}
		} else {
			if (newFolderInfo != null && newFolderInfo.getStatus() == AssetFolderStatus.VALID_WITH_ASSET) {
				throw new IllegalArgumentException("Asset folder status cannot be VALID_WITH_ASSET because properties are null! AssetFolder: '" + newFolderInfo.getAssetFolder() + "'");
			}
		}
		this.newestAssetProps = assetProps;
		this.newAssetFolderInfo = newFolderInfo;
	}
	
	public A getNewAssetProperties() {
		return newestAssetProps;
	}
	
	public NewAssetFolderInfo getNewAssetFolderInfo() {
		return newAssetFolderInfo;
	}
	
	public String getNewestAssetCacheKey() {
		if (newestAssetLastUpdate == null) return "";
		else return "" + newestAssetLastUpdate.toEpochMilli();
	}
}
