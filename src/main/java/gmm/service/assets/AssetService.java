package gmm.service.assets;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import gmm.collections.HashSet;
import gmm.collections.List;
import gmm.collections.Set;
import gmm.domain.Linkable;
import gmm.domain.task.Task;
import gmm.domain.task.asset.AssetGroupType;
import gmm.domain.task.asset.AssetName;
import gmm.domain.task.asset.AssetProperties;
import gmm.domain.task.asset.AssetTask;
import gmm.domain.task.asset.FileType;
import gmm.service.FileService;
import gmm.service.assets.NewAssetFolderInfo.AssetFolderStatus;
import gmm.service.assets.vcs.VcsPlugin;
import gmm.service.data.DataAccess;
import gmm.service.data.DataAccess.DataChangeCallback;
import gmm.service.data.DataChangeEvent;
import gmm.service.data.DataConfigService;
import gmm.service.tasks.AssetTaskService;
import gmm.service.tasks.TaskServiceFinder;
import gmm.util.Util;

/**
 * Manages the relationships between AssetTasks's and their asset files. This service knows which
 * asset files exist, if they have a valid state and thus if they can/should be linked to an
 * AssetTask.
 * 
 * If an AssetTask's assetName changes, this service must be told.
 * If an asset file changes, this service must be told (it provides methods to check if they did).
 * 
 * If an AssetTask or asset file changed, this service will determine what needs to happen:
 * - Adding / removing linked asset files to / from AssetTasks.
 * - Creating / Updating / Deleting asset file previews.
 * - Notify file changes to VcsPlugin, if the VcsPlugin is not responsible for the changes in the
 *   first place.
 * 
 * This service assumes that original asset files with same filename do not change their content,
 * that original assets are not added/removed/moved during the lifetime of this service and that
 * original asset folder structure is valid (no multiple asset files with same filename etc.)
 * 
 * @author Jan Mothes
 */
@Service
public class AssetService {
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private final VcsPlugin vcs;
	private final AssetScanner scanner;
	private final TaskServiceFinder serviceFinder;
	
	private final Map<AssetName, AssetTask<?>> assetTasks;
	
	private final Map<AssetName, NewAssetFolderInfo> newAssetFolders;
	// TODO make sure GUI errors are created whenever an invalid info is set or whenever
	// a task has his mapping changed / a task is added
	// TODO at the same time the tasks knowledge about wether it has a new asset folder must be updated
	// TODO edit event must be triggered for given tasks so they get updated in GUI
	
	private final Map<AssetName, OriginalAssetFileInfo> originalAssetFiles;
	
	private final DataChangeCallback reference;
	
	public NewAssetFolderInfo getNewAssetFolderInfo(AssetName assetName) {
		return newAssetFolders.get(assetName);
	}
	
	public OriginalAssetFileInfo getOriginalAssetFileInfo(AssetName assetName) {
		return originalAssetFiles.get(assetName);
	}
	
	@Autowired
	public AssetService(AssetScanner scanner, TaskServiceFinder serviceFinder, VcsPlugin vcs,
			DataConfigService config, FileService fileService, DataAccess data) {
		this.scanner = scanner;
		this.serviceFinder = serviceFinder;
		this.vcs = vcs;
		
		this.config = config;
		this.fileService = fileService;
		
		assetTasks = new HashMap<>();
		newAssetFolders = new HashMap<>();
		originalAssetFiles = new HashMap<>();
		
		reference = initTasksAndGetPostProcessor(data);
		data.registerPostProcessor(reference);
		
		onOriginalAssetFilesChanged();
		vcs.registerFilesChangedHandler(this);
		
		// TODO make sure that exactly one vcs service bean exists before this service gets constructed
		// and throw an error that tells user to check his vcs config (set it to none)
		// maybe even tell user list of possible plugins if possible
	}
	
	private DataChangeCallback initTasksAndGetPostProcessor(DataAccess data) {
		for (final AssetTask<?> assetTask : data.getList(AssetTask.class)) {
			assetTasks.put(assetTask.getAssetName(), assetTask);
		}
		return this::onDataChangeEvent;
	}
	
	private void onDataChangeEvent(DataChangeEvent event) {
		// TODO should DataChangeEvent only have concrete classes as generic type for easier checking?
		final Class<? extends Linkable> clazz = event.changed.getGenericType();
		if (Task.class.isAssignableFrom(clazz)) {
			switch(event.type) {
			case ADDED:
				for (final Task task : event.getChanged(Task.class)) {
					if (task instanceof AssetTask) {
						onAssetTaskCreation((AssetTask<?>) task);
					}
				}
				break;
			case EDITED:
				// TODO AssetService needs to get notified manually on AssetName change,
				// see AssetTaskService todo, AssetService needs old AssetName to cleanup
				break;
			case REMOVED:
				for (final Task task : event.getChanged(Task.class)) {
					if (task instanceof AssetTask) {
						onAssetTaskDeletion((AssetTask<?>) task);
					}
				}
				break;
			}
		}
	}
	
	/**
	 * Not needed for loaded tasks since their original properties dont change
	 */
	private <A extends AssetProperties> void onAssetTaskCreation(AssetTask<A> task) {
		final AssetName name = task.getAssetName();
		assetTasks.put(name, task);
		
		final AssetTaskService<A> service = serviceFinder.getAssetService(Util.classOf(task));
		
		{
			final AssetGroupType type = AssetGroupType.ORIGINAL;
			final OriginalAssetFileInfo info = getOriginalAssetFileInfo(name);
			final AssetProperties props = task.getAssetProperties(type);
			
			if (info != null && props == null) service.recreateAssetProperties(task, info);
			if (info == null && props != null) service.removeAssetProperties(task, type);
		}{
			final AssetGroupType type = AssetGroupType.NEW;
			final NewAssetFolderInfo info = getNewAssetFolderInfo(name);
			final AssetProperties props = task.getAssetProperties(type);
			
			final boolean existsAndValid =
					(info != null) && (info.getStatus() == AssetFolderStatus.VALID_WITH_ASSET);
			
			if (existsAndValid && props == null) service.recreateAssetProperties(task, info);
			if (existsAndValid && props != null && !service.isValidAssetProperties(props, info)) {
				service.recreateAssetProperties(task, info);
			}
			if (!existsAndValid && props != null) service.removeAssetProperties(task, type);
		}
	}
	
	private <A extends AssetProperties> void onAssetTaskDeletion(AssetTask<A> task) {
		final AssetName name = task.getAssetName();
		assetTasks.remove(name);
		
		final AssetTaskService<A> service = serviceFinder.getAssetService(Util.classOf(task));
		
		for (final AssetGroupType type : AssetGroupType.values()) {
			if (task.getAssetProperties(type) == null) return;
			else {
				service.removeAssetProperties(task, type);
			}
		}
	}
	
	public void onOriginalAssetFilesChanged() {
		applyFoundOriginal(scanner.onOriginalAssetFilesChanged());
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void applyFoundOriginal(Map<AssetName, OriginalAssetFileInfo> foundOriginalAssetFiles) {
		
		final AssetGroupType type = AssetGroupType.ORIGINAL;
		final Set<AssetName> oldFiles = new HashSet<>(AssetName.class, originalAssetFiles.keySet());
		
		foundOriginalAssetFiles.forEach((fileName, currentInfo) -> {
			
			final AssetTask<?> task = assetTasks.get(fileName);
			if (task != null) {
				final OriginalAssetFileInfo oldInfo = originalAssetFiles.get(fileName);
				final AssetTaskService<?> service = serviceFinder.getAssetService(Util.classOf(task));
				if (oldInfo == null) {
					// new assets
					service.recreateAssetProperties((AssetTask)task, currentInfo);
				} else {
					// changed assets
					final AssetProperties props = task.getAssetProperties(type);
					if (!service.isValidAssetProperties(props, currentInfo)) {
						service.recreateAssetProperties((AssetTask)task, currentInfo);
					}
				}
			}
			// prepare for removing
			oldFiles.remove(fileName);
			originalAssetFiles.put(fileName, currentInfo);
		});
		
		// removed assets
		for (final AssetName notFound : oldFiles) {
			final AssetTask<?> task = assetTasks.get(notFound);
			if (task != null) {
				final AssetTaskService<?> service = serviceFinder.getAssetService(notFound);
				service.removeAssetProperties((AssetTask)task, type);
			}
		}
	}
	
	
	/**
	 * Notify AssetService of changes to new asset files. Triggers a rescan of new asset files.
	 * 
	 * @param changedPaths - Calling this method with an empty list will detect some changes, but if
	 * 		a file changed its content without changing is filename, its path needs to be in the
	 * 		given list. Paths must be relative to new asset folder base (config.assetsNew()).
	 */
	public void onVcsNewAssetFilesChanged(List<Path> changedPaths) {
		applyFoundNew(scanner.onNewAssetFilesChanged(), changedPaths);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void applyFoundNew(Map<AssetName, NewAssetFolderInfo> foundNewAssetFolders, List<Path> changedPaths) {
		
		scanner.filterForNewAssets(changedPaths);
		
		final AssetGroupType type = AssetGroupType.NEW;
		final Set<AssetName> oldFolders = new HashSet<>(AssetName.class, originalAssetFiles.keySet());
		
		final BiConsumer<AssetName, NewAssetFolderInfo> applyEntry = (folderName, currentInfo) -> {
			
			final AssetTask<?> task = assetTasks.get(folderName);
			if (task != null) {
				final NewAssetFolderInfo oldInfo = newAssetFolders.get(folderName);
				final AssetTaskService<?> service = serviceFinder.getAssetService((Class)Util.getClass(task));
				if (oldInfo == null) {
					service.recreateAssetProperties((AssetTask)task, currentInfo);
				} else {
					final AssetFolderStatus hasAsset = AssetFolderStatus.VALID_WITH_ASSET;
					final boolean oldHasAsset = oldInfo.getStatus() == hasAsset;
					final boolean currentHasAsset = currentInfo.getStatus() == hasAsset;
					
					if (oldHasAsset && !currentHasAsset) {
						service.removeAssetProperties((AssetTask)task, type);
					} else if (!oldHasAsset && currentHasAsset) {
						service.recreateAssetProperties((AssetTask)task, currentInfo);
					} else if (oldHasAsset && currentHasAsset) {
						
						final AssetProperties props = task.getAssetProperties(type);
						if (!service.isValidAssetProperties(props, currentInfo)) {
							service.recreateAssetProperties((AssetTask)task, currentInfo);
						} else {
							final String assetName = currentInfo.getAssetFileName().get();
							final Path assetFile = currentInfo.getAssetFolder().resolve(assetName);
							if (changedPaths.contains(assetFile)) {
								service.recreateAssetProperties((AssetTask)task, currentInfo);
								changedPaths.remove(assetFile);
							}
						}
					} else {
						// both are not hasAsset, so properties should not exist anyway
					}
				}
			}
			// prepare for removing
			oldFolders.remove(folderName);
			newAssetFolders.put(folderName, currentInfo);
		};
		
		foundNewAssetFolders.forEach(applyEntry);
		
		// removed assets
		for (final AssetName notFound : oldFolders) {
			final AssetTask<?> task = assetTasks.get(notFound);
			if (task != null && task.getAssetProperties(type) != null) {
				final AssetTaskService<?> service = serviceFinder.getAssetService(notFound);
				service.removeAssetProperties((AssetTask)task, type);
			}
		}
	}
	
	final DataConfigService config;
	final FileService fileService;
	
	public void deleteAssetFile(AssetName assetFolderName) {
		deleteOtherFile(assetFolderName, FileType.ASSET, null);
	}
	
	public void deleteOtherFile(AssetName assetFolderName, FileType fileType, Path relativeFile) {
		
		final NewAssetFolderInfo folderInfo = getNewAssetFolderInfo(assetFolderName);
		Assert.isTrue(folderInfo.getStatus().isValid);
		
		final Path absoluteFile;
		
		if (fileType.isAsset()) {
			Assert.isTrue(folderInfo.getStatus() == AssetFolderStatus.VALID_WITH_ASSET);
			absoluteFile = folderInfo.getAssetFilePathAbsolute(config);
		} else {
			final Path assetFolder = config.assetsNew().resolve(folderInfo.getAssetFolder());
			final Path visible = assetFolder.resolve(fileType.getSubPath(config));
			absoluteFile = visible.resolve(fileService.restrictAccess(relativeFile, visible));
		}
		logger.info("Deleting file from new asset folder at '" + absoluteFile + "'");
		
		fileService.delete(absoluteFile);
		vcs.commitRemovedFile(config.assetsNew().relativize(absoluteFile));
		
		if (fileType.isAsset()) {
			final AssetTask<?> task = assetTasks.get(assetFolderName);
			if (task != null && task.getAssetProperties(AssetGroupType.NEW) != null) {
				serviceFinder.getAssetService(Util.classOf(task)).removeAssetProperties((AssetTask)task, AssetGroupType.NEW);
			}
		}
	}
}
