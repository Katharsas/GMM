package gmm.service.assets.vcs;

import java.nio.file.Path;

import org.springframework.stereotype.Service;

import gmm.collections.ArrayList;
import gmm.service.assets.vcs.VcsPluginSelector.ConditionalOnConfigSelector;

@Service
@ConditionalOnConfigSelector({"","none"})
public class NonePlugin extends VcsPlugin {

	@Override
	protected void assertToken() {}
	
	@Override
	public boolean isCustomAssetPathsAllowed() {
		return false;
	}

	@Override
	public void init() {
		onFilesChanged(new ArrayList<>(Path.class, 0));
	}
	
	@Override
	public void notifyRepoChange() {
	}

	@Override
	public void addFile(Path file) {
	}

	@Override
	public void editFile(Path file) {
	}

	@Override
	public void removeFile(Path file) {
	}

	@Override
	public void commit(String message) {
	}
}
