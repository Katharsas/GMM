package gmm.web.sessions;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import gmm.collections.Collection;
import gmm.collections.LinkedList;
import gmm.collections.List;
import gmm.domain.task.Task;
import gmm.domain.task.asset.Asset;
import gmm.domain.task.asset.AssetTask;
import gmm.domain.task.asset.ModelTask;
import gmm.domain.task.asset.TextureTask;
import gmm.service.ajax.BundledMessageResponses;
import gmm.service.ajax.MessageResponse;
import gmm.service.ajax.operations.AssetImportOperations;
import gmm.service.data.DataAccess;
import gmm.web.forms.TaskForm;

/**
 * Represents the current state of the asset selection the user has made for asset import.
 * 
 * @author Jan Mothes
 */
@Component
@Scope(value="session", proxyMode=ScopedProxyMode.TARGET_CLASS)
public class AdminSession {
	
	@Autowired private DataAccess data;
	
	public void cleanUp() {
		taskLoader = null;
		assetImporter = null;
		clearImportPaths();
	}

	/*--------------------------------------------------
	 * Load tasks from file
	 * ---------------------------------------------------*/
	
	public BundledMessageResponses<? extends Task> taskLoader;
	
	/*--------------------------------------------------
	 * Import asset tasks
	 * ---------------------------------------------------*/
	
	private BundledMessageResponses<String> assetImporter;
	private final List<String> importFilePaths = new LinkedList<>(String.class);
	private boolean areTexturePaths = true;
	
	// Add asset paths
	
	public void addImportPaths(Collection<String> paths, boolean areTexturePaths) {
		if(this.areTexturePaths != areTexturePaths) {
			importFilePaths.clear();
			this.areTexturePaths = areTexturePaths;
		}
		for (final String path : paths) {
			if (!importFilePaths.contains(path)) {
				importFilePaths.add(path);
			}
		}
	}
	public void clearImportPaths() {
		importFilePaths.clear();
	}
	
	public List<String> getImportPaths() {
		return importFilePaths.copy();
	}
	
	// Check asset paths for conflict
	
	public List<MessageResponse> firstImportCheckBundle(TaskForm form) {
		final Class<? extends AssetTask<?>> type = areTexturePaths ?
				TextureTask.class : ModelTask.class;
		final AssetImportOperations<? extends Asset, ? extends AssetTask<?>> ops =
				new AssetImportOperations<>(form, type, data::add);
		
		assetImporter = new BundledMessageResponses<>(
				getImportPaths(), ops, ()->{assetImporter = null;});
		
		return assetImporter.loadFirstBundle();
	}
	
	public List<MessageResponse> nextImportCheckBundle(String operation, boolean doForAllFlag) {
		return assetImporter.loadNextBundle(operation, doForAllFlag);
	}
}
