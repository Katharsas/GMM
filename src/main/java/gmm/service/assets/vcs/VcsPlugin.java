package gmm.service.assets.vcs;

import java.nio.file.Path;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gmm.collections.List;
import gmm.service.assets.AssetService;

public abstract class VcsPlugin {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private AssetService filesChangedHandler;
	
	public VcsPlugin() {
		logger.debug("\n"
				+ "##########################################################" + "\n\n"
				+ "  Version Control Plugin:" + "\n"
				+ "  " + this.getClass().getSimpleName() + "\n\n"
				+ "##########################################################");
	}
	
	public void registerFilesChangedHandler(AssetService assetService) {
		Objects.requireNonNull(assetService);
		if (filesChangedHandler != null) {
			throw new IllegalStateException("AssetService has already been registered!");
		}
		filesChangedHandler = assetService;
		init();
	}
	
	/**
	 * Cannot be called from constructor. Call this from {@link #init()} or later.
	 */
	protected void onFilesChanged(List<Path> changedPaths) {
		if (filesChangedHandler == null) {
			throw new IllegalStateException("This method becomes available during init() method, not earlier!");
		}
		filesChangedHandler.onVcsNewAssetFilesChanged(changedPaths);
	}
	
	/**
	 * Will be called once during startup as soon as other services are setup to work with this
	 * service properly. This method is guaranteed to be able to call {@link #onFilesChanged(List)}.
	 */
	public abstract void init();
	
	/**
	 * False indicates that new assets should be saved on same path as original ones, if existent.
	 * True indicates that new assets may have a different folder structure and user must specify
	 * the path for a new asset he uploads, even if an original exists for that asset.
	 */
	public abstract boolean isCustomAssetPathsAllowed();
	
	/**
	 * Allows plugin to not rely on polling but instead get notified directly if the VCS server / repo changes.
	 */
	public abstract void notifyRepoChange();
	
	public abstract void commitAddedFile(Path file);
	
	public abstract void commitChangedFile(Path file);
	
	public abstract void commitRemovedFile(Path file);
}
