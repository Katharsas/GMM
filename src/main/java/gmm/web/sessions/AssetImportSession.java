package gmm.web.sessions;

import gmm.collections.Collection;
import gmm.collections.LinkedList;
import gmm.collections.List;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

/**
 * Represents the current state of the asset selection the user has made for asset import.
 * 
 * @author Jan Mothes
 */
@Component
@Scope(value="session", proxyMode=ScopedProxyMode.TARGET_CLASS)
public class AssetImportSession {

	private final List<String> filePaths = new LinkedList<>();
	private boolean areTexturePaths = true;
	
	public void addPaths(Collection<String> paths, boolean areTexturePaths) {
		if(this.areTexturePaths != areTexturePaths) {
			filePaths.clear();
			this.areTexturePaths = areTexturePaths;
		}
		filePaths.addAll(paths);
	}
	public void clear() {
		filePaths.clear();
	}
	
	public List<String> get() {
		return filePaths.copy();
	}
}
