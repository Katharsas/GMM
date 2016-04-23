package gmm.web.sessions;


import java.util.HashMap;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import gmm.collections.Collection;
import gmm.collections.HashSet;
import gmm.collections.LinkedList;
import gmm.collections.List;
import gmm.domain.task.GeneralTask;
import gmm.domain.task.Task;
import gmm.domain.task.asset.Asset;
import gmm.domain.task.asset.AssetGroupType;
import gmm.domain.task.asset.AssetTask;
import gmm.domain.task.asset.ModelTask;
import gmm.domain.task.asset.TextureTask;
import gmm.service.ajax.BundledMessageResponses;
import gmm.service.ajax.MessageResponse;
import gmm.service.ajax.operations.AssetImportOperations;
import gmm.service.ajax.operations.TaskLoaderOperations;
import gmm.service.data.DataAccess;
import gmm.service.tasks.AssetTaskService;
import gmm.service.tasks.TaskServiceFinder;
import gmm.util.Util;
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
	@Autowired private TaskServiceFinder serviceFinder;

	/*--------------------------------------------------
	 * Load tasks from file
	 * ---------------------------------------------------*/
	
	private BundledMessageResponses<? extends Task> generalTaskLoader;
	private BundledMessageResponses<String> assetTaskLoader;
	private Multimap<Class<? extends Task>, Task> multiMap;
	
	public void prepareLoadTasks(Collection<Task> tasks) {
		// split tasks into types to use correct conflict checker for loading
		multiMap = HashMultimap.create();
		StreamSupport.stream(tasks.spliterator(), false)
			.forEach(task -> multiMap.put(task.getClass(), task));
	}
	
	public List<MessageResponse> firstLoadGeneralCheckBundle() {
		java.util.Collection<Task> tasks = multiMap.get(GeneralTask.class);
		
		final TaskLoaderOperations ops = new TaskLoaderOperations();
		generalTaskLoader = new BundledMessageResponses<>(
				tasks, ops, ()->{generalTaskLoader = null;});
		
		return generalTaskLoader.loadFirstBundle();
	}
	
	public List<MessageResponse> firstLoadAssetCheckBundle() {
		final Collection<Task> assetTasks = new HashSet<>(Task.class);
		assetTasks.addAll(multiMap.get(TextureTask.class));
		assetTasks.addAll(multiMap.get(ModelTask.class));
		
		HashMap<String, AssetTask<?>> pathToTask = new HashMap<>();
		for(Task task : assetTasks) {
			AssetTask<?> assetTask = (AssetTask<?>) task;
			pathToTask.put(assetTask.getAssetPath().toString(), assetTask);
		}
		final AssetImportOperations<?> ops = new AssetImportOperations<>(
				AssetTask.class, (path) -> {
					AssetTask<?> task = pathToTask.get(path);
					task.onLoad();
					return updateAssetTaskAssets(task);
				}, data::add);
		
		assetTaskLoader = new BundledMessageResponses<String>(
				pathToTask.keySet(), ops, ()->{assetTaskLoader = null;});
		
		return assetTaskLoader.loadFirstBundle();
	}
	
	private <A extends Asset> AssetTask<A> updateAssetTaskAssets(AssetTask<A> task) {
		AssetTaskService<A> service = serviceFinder.getAssetService(Util.classOf(task));
		service.updateAssetUpdatePreview(task, AssetGroupType.ORIGINAL);
		service.updateAssetUpdatePreview(task, AssetGroupType.NEW);
		return task;
	}
	
	public List<MessageResponse> nextLoadCheckBundle(String operation, boolean doForAllFlag) {
		return (generalTaskLoader != null ? generalTaskLoader : assetTaskLoader)
				.loadNextBundle(operation, doForAllFlag);
	}

	
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
		final AssetImportOperations<?> ops = new AssetImportOperations<>(form, type, data::add);
		
		assetImporter = new BundledMessageResponses<>(
				getImportPaths(), ops, ()->{assetImporter = null;});
		
		return assetImporter.loadFirstBundle();
	}
	
	public List<MessageResponse> nextImportCheckBundle(String operation, boolean doForAllFlag) {
		return assetImporter.loadNextBundle(operation, doForAllFlag);
	}
}
