package gmm.service.assets.vcs;

import java.nio.file.Path;

import org.springframework.stereotype.Service;

import gmm.collections.ArrayList;
import gmm.service.assets.vcs.VcsPluginSelector.ConditionalOnConfigSelector;

@Service
@ConditionalOnConfigSelector("none")
public class NonePlugin extends VcsPlugin {

	@Override
	public boolean isCustomAssetPathsAllowed() {
		return false;
	}

	@Override
	public void init() {
		notifyFilesChanged(new ArrayList<>(Path.class, 0));
	}

	@Override
	public void commitAddedFile(Path file) {
	}

	@Override
	public void commitChangedFile(Path file) {
	}

	@Override
	public void commitRemovedFile(Path file) {
	}
}
