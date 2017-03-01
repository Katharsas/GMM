package gmm.service.assets.vcs;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gmm.collections.ArrayList;
import gmm.service.assets.AssetService;
import gmm.service.assets.vcs.VcsPluginSelector.ConditionalOnConfigSelector;

@Service
@ConditionalOnConfigSelector("none")
public class NonePlugin extends VcsPlugin {

	@Autowired
	public NonePlugin(AssetService assetService) {
		super(assetService);
		notifyFilesChanged(new ArrayList<>(Path.class, 0));
	}

	@Override
	public boolean allowCustomAssetPaths() {
		return false;
	}
}
