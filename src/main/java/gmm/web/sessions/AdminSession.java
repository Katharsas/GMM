package gmm.web.sessions;


import java.util.Iterator;

import gmm.collections.Collection;
import gmm.collections.LinkedList;
import gmm.collections.List;
import gmm.domain.task.Asset;
import gmm.domain.task.AssetTask;
import gmm.domain.task.ModelTask;
import gmm.domain.task.Task;
import gmm.domain.task.TextureTask;
import gmm.service.ajax.BundledMessageResponses;
import gmm.service.ajax.operations.AssetImportOperations;
import gmm.web.forms.TaskForm;

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
public class AdminSession {

	public BundledMessageResponses<? extends Task> taskLoader;
	public BundledMessageResponses<String> assetImporter;
	
	private AssetImportOperations<? extends Asset, ? extends AssetTask<?>> importOperations;
	private final List<String> importFilePaths = new LinkedList<>();
	private boolean areTexturePaths = true;
	
	public void addImportPaths(Collection<String> paths, boolean areTexturePaths) {
		if(this.areTexturePaths != areTexturePaths) {
			importFilePaths.clear();
			this.areTexturePaths = areTexturePaths;
		}
		for (String path : paths) {
			if (!importFilePaths.contains(path)) {
				importFilePaths.add(path);
			}
		}
	}
	public void clearImportPaths() {
		importOperations = null;
		taskLoader = null;
		assetImporter = null;
		importFilePaths.clear();
	}
	
	public List<String> getImportPaths() {
		return importFilePaths.copy();
	}
	
	public AssetImportOperations<? extends Asset, ? extends AssetTask<?>> getAssetImportOperations(TaskForm form) {
		if(areTexturePaths) {
			importOperations = new AssetImportOperations<>(form, TextureTask.class);
		}
		else {
			importOperations = new AssetImportOperations<>(form, ModelTask.class);
		}
		return importOperations;
	}
	
	public Iterator<? extends Task> getImportedTasks() {
		List<? extends Task> list = (List<? extends Task>) importOperations.getTasks();
		return list.iterator();
	}
}
