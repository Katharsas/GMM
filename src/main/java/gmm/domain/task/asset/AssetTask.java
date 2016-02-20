package gmm.domain.task.asset;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

import gmm.domain.User;
import gmm.domain.task.Task;
import gmm.service.Spring;
import gmm.service.data.DataConfigService;

/**
 * After creation & after loading the path configuration must be injected by calling {@link #setConfig(DataConfigService)}.
 * 
 * @author Jan Mothes
 *
 * @param <A> type of asset
 */
public abstract class AssetTask<A extends Asset> extends Task {

	@XStreamOmitField
	protected DataConfigService config;
	
	private final Path assetPath;
	private A originalAsset = null;
	private A newestAsset = null;
	
	//used for caching of newest preview
	private DateTime newestAssetLastUpdate = null;
	private final static DateTimeFormatter formatter = 
			DateTimeFormat.forPattern("MM-dd-HH-mm-ss").withLocale(Locale.ENGLISH);
	
	//Methods--------------------------------------------
	public AssetTask(User author, Path assetPath) {
		super(author);
		this.assetPath = assetPath;
	}
	
	@Override
	public void onLoad() {
		this.config = Spring.get(DataConfigService.class);
	}
	
	public Path getOriginalAssetPath() {
		return getFilePath(getOriginalAsset());
	}
	
	public Path getNewestAssetPath() {
		return getFilePath(getNewestAsset());
	}
	
	/**
	 * @return Absolute file path to the given asset file assuming the asset can be found under
	 * 		this AssetTask's relative path.
	 */
	private Path getFilePath(Asset asset) {
		Objects.requireNonNull(config);
		if(asset.getGroupType().isOriginal()) {
			return config.ASSETS_ORIGINAL.resolve(getAssetPath());
		} else {
			return config.ASSETS_NEW.resolve(getAssetPath())
					.resolve(config.SUB_ASSETS).resolve(asset.getFileName());
		}
	}
	
	//Setters, Getters---------------------------------
	
	public Path getAssetPath() {
		return assetPath;
	}
	
	public A getOriginalAsset() {
		return originalAsset;
	}

	public void setOriginalAsset(A originalAsset) {
		originalAsset.setFileSize(getFilePath(originalAsset));
		originalAsset.assertAttributes();
		this.originalAsset = originalAsset;
	}
	
	public A getNewestAsset() {
		return newestAsset;
	}
	
	public void setNewestAsset(A newestAsset) {
		newestAsset.setFileSize(getFilePath(newestAsset));
		newestAsset.assertAttributes();
		this.newestAsset = newestAsset;
		this.newestAssetLastUpdate = DateTime.now();
	}
	
	public String getNewestAssetNocache() {
		if (newestAssetLastUpdate == null) return "";
		else return newestAssetLastUpdate.toString(formatter);
	}
}
