package gmm.domain.task;

import gmm.domain.User;
import gmm.service.Spring;
import gmm.service.data.DataConfigService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

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
	public AssetTask(User author, Path assetPath) throws Exception {
		super(author);
		this.assetPath = assetPath;
	}
	
	@Override
	public void onLoad() throws Exception {
		this.config = Spring.get(DataConfigService.class);
	}
	
	public Path getOriginalAssetPath() {
		return config.ASSETS_ORIGINAL.resolve(getAssetPath());
	}
	
	public Path getNewestAssetPath() {
		return config.ASSETS_NEW.resolve(getAssetPath())
				.resolve(config.SUB_ASSETS).resolve(newestAsset.getFileName());
	}
	
	//Setters, Getters---------------------------------
	
	public Path getAssetPath() {
		return assetPath;
	}
	
	public A getOriginalAsset() {
		if (originalAsset != null) {
			originalAsset.setAbsolute(getOriginalAssetPath());
		}
		return originalAsset;
	}

	public void setOriginalAsset(A originalAsset) throws IOException {
		Objects.requireNonNull(config);
		this.originalAsset = originalAsset;
	}
	
	public A getNewestAsset() {
		if (newestAsset != null) {
			newestAsset.setAbsolute(getNewestAssetPath());
		}
		return newestAsset;
	}
	
	public void setNewestAsset(A newestAsset) throws IOException {
		Objects.requireNonNull(config);
		this.newestAsset = newestAsset;
		this.newestAssetLastUpdate = DateTime.now();
	}
	
	public String getNewestAssetNocache() {
		if (newestAssetLastUpdate == null) return "";
		else return newestAssetLastUpdate.toString(formatter);
	}
}
