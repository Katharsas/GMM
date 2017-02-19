package gmm.service.assets.vcs;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;

import gmm.collections.Collection;
import gmm.service.data.DataConfigService;

public abstract class VcsPlugin {

	private DataConfigService config;
	
	@Autowired
	public VcsPlugin(DataConfigService config) {
		this.config = config;
	}
	
	/**
	 * False indicates that new assets should be saved on same path as original ones, if existent.
	 * True indicates that new assets may have a different folder structure and user must specify
	 * the path for a new asset he uploads, even if an original exists for that asset.
	 */
	public abstract boolean allowCustomAssetPaths();
	
	public void notifyRepositoryChanged(Collection<Path> changedFiles) {
		// TODO filter files to paths only under asset type folders
		// TODO get only asset file changes (check parent folder for each file, must be asset folder and not inside another asset folder)
	}
}
