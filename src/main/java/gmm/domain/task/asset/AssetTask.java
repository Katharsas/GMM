package gmm.domain.task.asset;

import java.util.Locale;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import gmm.domain.User;
import gmm.domain.task.Task;

/**
 * An asset task may contain a path to an original asset. In this case, both the path and the 
 * {@link AssetProperties} for the original asset are both not-null, otherwise not null.
 * 
 * An asset task may contain a path to an assetFolder for a new asset. If it has, it may also
 * contain {@link AssetProperties} for a new asset inside the assetFolder. If it hasn't, it cannot.
 * 
 * @author Jan Mothes
 *
 * @param <A> type of asset
 */
public abstract class AssetTask<A extends AssetProperties> extends Task {
	
	private final AssetName assetName;
	
	private A originalAsset = null;
	private A newestAsset = null;
	
	private NewAssetFolderError newAssetFolderError = null;
	private boolean hasNewAssetFolder = false;
	
	//used for caching of newest preview
	private DateTime newestAssetLastUpdate = null;
	private final static DateTimeFormatter formatter = 
			DateTimeFormat.forPattern("MM-dd-HH-mm-ss").withLocale(Locale.ENGLISH);
	
	//Methods--------------------------------------------
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
	
	public void setAssetProperties(A assetProps, AssetGroupType type) {
		if (type.isOriginal()) setOriginalAsset(assetProps);
		else setNewAsset(assetProps);
	}
	
	public A getAssetProperties(AssetGroupType type) {
		return type.isOriginal() ? getOriginalAsset() : getNewAsset();
	}
	
	public void setOriginalAsset(A assetProps) {
		if (assetProps != null) {
			assetProps.assertAttributes();
		}
		this.originalAsset = assetProps;
	}
	
	public A getOriginalAsset() {
		return originalAsset;
	}
	
	public void setNewAsset(A asset) {
		if(asset != null) {
			asset.assertAttributes();
		}
		this.newestAsset = asset;
		this.newestAssetLastUpdate = DateTime.now();
	}
	
	public A getNewAsset() {
		return newestAsset;
	}
	
	public String getNewestAssetNocache() {
		if (newestAssetLastUpdate == null) return "";
		else return newestAssetLastUpdate.toString(formatter);
	}
	
	public void setHasNewAssetFolder(boolean hasNewAssetFolder) {
		this.hasNewAssetFolder = hasNewAssetFolder;
	}
	
	public boolean hasNewAssetFolder() {
		return hasNewAssetFolder;
	}
	
	public void setNewAssetFolderError(NewAssetFolderError newAssetFolderError) {
		this.newAssetFolderError = newAssetFolderError;
	}
	
	public NewAssetFolderError getNewAssetFolderError() {
		return newAssetFolderError;
	}
}
