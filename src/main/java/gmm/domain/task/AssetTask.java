package gmm.domain.task;

import gmm.domain.User;
import gmm.service.Spring;
import gmm.service.data.DataConfigService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

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
	}
}
