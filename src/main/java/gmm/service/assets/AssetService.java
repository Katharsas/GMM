package gmm.service.assets;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import gmm.collections.Collection;
import gmm.collections.HashSet;
import gmm.collections.List;
import gmm.collections.Set;
import gmm.collections.UnmodifiableCollection;
import gmm.domain.User;
import gmm.domain.task.asset.AssetGroupType;
import gmm.domain.task.asset.AssetName;
import gmm.domain.task.asset.AssetProperties;
import gmm.domain.task.asset.AssetTask;
import gmm.domain.task.asset.FileType;
import gmm.service.FileService;
import gmm.service.assets.AssetTaskUpdater.OnNewAssetUpdate;
import gmm.service.assets.AssetTaskUpdater.OnOriginalAssetUpdate;
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
	private final AssetTaskUpdater taskUpdater;
	private final DataAccess data;
	private final NewAssetLockService lockService;
	
	// TODO currently, there is a small preview leak, because new assets are scanned for the first
	// time AFTER VcsPlugin updated the WC. This means that any new assets that were deleted during
	// offline GMM will still have previews left and cannot be found because AssetInfo is missing in
	// new scan. This could be fixed by scanning once more before initializing VcsPlugin. AssetInfo
	// would then be removed together with previews when scanning updated WC.
	
	private final Map<AssetName, AssetTask<?>> assetTasks;
	
	private final Map<AssetName, NewAssetFolderInfo> newAssetFolders;
	private final Map<AssetName, NewAssetFolderInfo> newAssetFoldersWithoutTasks;
	
	private final Map<AssetName, OriginalAssetFileInfo> originalAssetFiles;
	
	private final DataChangeCallback<AssetTask<?>> reference;
	
	public NewAssetFolderInfo getNewAssetFolderInfo(AssetName assetName) {
		return newAssetFolders.get(assetName);
	}
	
	public Collection<NewAssetFolderInfo> getNewAssetFoldersWithoutTasks() {
		return new UnmodifiableCollection<>(NewAssetFolderInfo.class, newAssetFoldersWithoutTasks.values());
	}
	
	public OriginalAssetFileInfo getOriginalAssetFileInfo(AssetName assetName) {
		return originalAssetFiles.get(assetName);
	}
	
	@Autowired
	public AssetService(AssetScanner scanner, TaskServiceFinder serviceFinder, AssetTaskUpdater taskUpdater,
			VcsPlugin vcs, DataConfigService config, FileService fileService, DataAccess data, NewAssetLockService lockService) {
		this.scanner = scanner;
		this.serviceFinder = serviceFinder;
		this.taskUpdater = taskUpdater;
		this.data = data;
		this.vcs = vcs;
		this.lockService = lockService;
		
		this.config = config;
		this.fileService = fileService;
		
		assetTasks = new ConcurrentHashMap<>();
		newAssetFolders = new ConcurrentHashMap<>();
		newAssetFoldersWithoutTasks = new ConcurrentHashMap<>();
		originalAssetFiles = new ConcurrentHashMap<>();
		
		fileService.createDirectory(config.assetsNew());
		fileService.createDirectory(config.assetsOriginal());
		
		reference = initTasksAndGetPostProcessor(data);
		data.registerPostProcessor(reference, AssetTask.getGenericClass());
		
		onOriginalAssetFilesChanged();
		vcs.registerFilesChangedHandler(this);
	}
	
	private DataChangeCallback<AssetTask<?>> initTasksAndGetPostProcessor(DataAccess data) {
		for (final AssetTask<?> assetTask : data.getList(AssetTask.class)) {
			assetTasks.put(assetTask.getAssetName(), assetTask);
		}
		return event -> onDataChangeEvent(event);
	}
	
	private void onDataChangeEvent(DataChangeEvent<AssetTask<?>> event) {
		switch(event.type) {
		case ADDED:
			try {
				lockService.lock();
				for (final AssetTask<?> task : event.changed) {
					onAssetTaskCreation((AssetTask<?>) task);
				}
				break;
			} finally {
				lockService.unlock();
			}
		case EDITED:
			// TODO AssetService needs to get notified manually on AssetName change,
			// see AssetTaskService todo, AssetService needs old AssetName to cleanup
			break;
		case REMOVED:
			for (final AssetTask<?> task : event.changed) {
				onAssetTaskDeletion((AssetTask<?>) task);
			}
			break;
		}
	}
	
	/**
	 * Synchronize an asset task and its property information with existing assets.
	 */
	private <A extends AssetProperties> void onAssetTaskCreation(AssetTask<A> task) {
		final AssetName name = task.getAssetName();
		assetTasks.put(name, task);
		
		final AssetTaskService<A> service = serviceFinder.getAssetService(Util.classOf(task));
		
		{
			final AssetGroupType type = AssetGroupType.ORIGINAL;
			final OriginalAssetFileInfo info = getOriginalAssetFileInfo(name);
			final AssetProperties props = task.getAssetProperties(type);
			
			final OnOriginalAssetUpdate updater = taskUpdater.new OnOriginalAssetUpdate(()->{
				data.editBy(task, User.SYSTEM);
			});
			
			if (info != null && props == null) {
				updater.recreatePropsAndSetInfo(task, info);
			}
			if (info != null && props != null && !service.isValidAssetProperties(props, info)) {
				updater.recreatePropsAndSetInfo(task, info);
			}
			if (info == null && props != null) {
				updater.removePropsAndInfo(task);
			}
		}{
			final AssetGroupType type = AssetGroupType.NEW;
			final NewAssetFolderInfo info = getNewAssetFolderInfo(name);
			
			if (info != null) newAssetFoldersWithoutTasks.remove(name);
			
			final AssetProperties props = task.getAssetProperties(type);
			
			final boolean existsAndValid =
					(info != null) && (info.getStatus() == AssetFolderStatus.VALID_WITH_ASSET);
			
			final OnNewAssetUpdate updater = taskUpdater.new OnNewAssetUpdate(() -> {
				data.editBy(task, User.SYSTEM);
			});
			
			if (props == null) {
				// old props don't exist, valid asset exists => recreate properties
				if (existsAndValid) {
					updater.recreatePropsAndSetInfo(task, info);
				} else {
					// old props don't exist, invalid asset exist => set invalid info
					// old props don't exist, invalid asset does not exist => set invalid info null
					
					final boolean bothNull = task.getNewAssetFolderInfo() == null && info == null;
					final boolean sameInvalid = task.getNewAssetFolderInfo() != null && info != null
							&& task.getNewAssetFolderInfo().getStatus().equals(info.getStatus()); 
					if (!bothNull && !sameInvalid) {
						updater.setInfo(task, Optional.ofNullable(info));
					}
				}
			} else {
				if (existsAndValid) {
					// old props exist, valid asset exist but is different => recreate properties
					if (!service.isValidAssetProperties(props, info)) {
						updater.recreatePropsAndSetInfo(task, info);
					}
				} else {
					// old props exist, invalid asset does not exist => remove properties & remove info
					// old props exist, invalid asset exist => remove properties & set invalid info
					
					updater.removePropsAndSetInfo(task, Optional.ofNullable(info));
				}
			}
		}
	}
	
	private <A extends AssetProperties> void onAssetTaskDeletion(AssetTask<A> task) {
		final AssetName name = task.getAssetName();
		assetTasks.remove(name);
		
		final NewAssetFolderInfo newFolderInfo = newAssetFolders.get(name);
		if (newFolderInfo != null) newAssetFoldersWithoutTasks.put(name, newFolderInfo);
		
		if (task.getOriginalAssetProperties() != null) {
			Objects.requireNonNull(task.getOriginalAssetFileInfo());
			taskUpdater.new OnOriginalAssetUpdate().removePropsAndInfo(task);
		}
		if (task.getNewAssetProperties() != null) {
			taskUpdater.new OnNewAssetUpdate().removePropsAndSetInfo(task, Optional.empty());
		}
	}
	
	private void onOriginalAssetFilesChanged() {
		applyFoundOriginal(scanner.onOriginalAssetFilesChanged());
	}
	
	private void applyFoundOriginal(Map<AssetName, OriginalAssetFileInfo> foundOriginalAssetFiles) {
		
		final AssetGroupType type = AssetGroupType.ORIGINAL;
		final Set<AssetName> oldFiles = new HashSet<>(AssetName.class, originalAssetFiles.keySet());
		
		foundOriginalAssetFiles.forEach((fileName, currentInfo) -> {
			
			final AssetTask<?> task = assetTasks.get(fileName);
			if (task != null) {
				final OriginalAssetFileInfo oldInfo = originalAssetFiles.get(fileName);
				final AssetTaskService<?> service = serviceFinder.getAssetService(Util.classOf(task));
				final OnOriginalAssetUpdate update = taskUpdater.new OnOriginalAssetUpdate(()->{
					data.edit(task);
				});
				if (oldInfo == null) {
					// new assets
					update.recreatePropsAndSetInfo(task, currentInfo);
				} else {
					// changed assets
					final AssetProperties props = task.getAssetProperties(type);
					if (!service.isValidAssetProperties(props, currentInfo)) {
						update.recreatePropsAndSetInfo(task, currentInfo);
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
				final OnOriginalAssetUpdate update = taskUpdater.new OnOriginalAssetUpdate(()->{
					data.edit(task);
				});
				if (task.getAssetProperties(type) != null) {
					update.removePropsAndInfo(task);
				}
			}
			originalAssetFiles.remove(notFound);
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
		final Set<AssetName> oldFolders = new HashSet<>(AssetName.class, newAssetFolders.keySet());
		
		final BiConsumer<AssetName, NewAssetFolderInfo> applyEntry = (folderName, currentInfo) -> {
			
			final NewAssetFolderInfo oldInfo = newAssetFolders.get(folderName);
			final AssetFolderStatus hasAsset = AssetFolderStatus.VALID_WITH_ASSET;
			final boolean oldHasAsset = oldInfo != null && oldInfo.getStatus() == hasAsset;
			final boolean currentHasAsset = currentInfo.getStatus() == hasAsset;
			
			// update tasks
			final AssetTask<?> task = assetTasks.get(folderName);
			if (task == null) {
				newAssetFoldersWithoutTasks.put(folderName, currentInfo);
			} else {
				final AssetTaskService<?> service = serviceFinder.getAssetService((Class)Util.getClass(task));
				
				final OnNewAssetUpdate updater = taskUpdater.new OnNewAssetUpdate(() -> {
					data.editBy(task, User.UNKNOWN);
					// TODO calling edit causes the onDataChangeEvent above to be called, so we could just do everything in there
					// (the old info would be available through tasks), same goes for original tasks
				});
				
				// asset was removed
				if (oldHasAsset && !currentHasAsset) {
					assertTaskHasAssetProperties(task, type);
					updater.removePropsAndSetInfo(task, Optional.of(currentInfo));
				// asset was added
				} else if (!oldHasAsset && currentHasAsset) {
					updater.recreatePropsAndSetInfo(task, currentInfo);
				// asset still exists
				} else if (oldHasAsset && currentHasAsset) {
					assertTaskHasAssetProperties(task, type);
					final AssetProperties props = task.getAssetProperties(type);
					// file path hasn't changed, content has changed
					if (changedPaths.contains(currentInfo.getAssetFilePath(config).normalize())) {
						updater.recreatePropsAndSetInfo(task, currentInfo);
					}
					// file path may have changed, content may have changed
					else if (!service.isValidAssetProperties(props, currentInfo)) {
						updater.recreatePropsAndSetInfo(task, currentInfo);
					// file path may have changed, content hasn't changed
					} else {
						if (!Objects.equals(oldInfo, currentInfo)) {
							updater.setInfo(task, Optional.of(currentInfo));
						}
					}
				// asset still does not exist, but folder status may have changed
				} else {
					if (!Objects.equals(oldInfo, currentInfo)) {
						updater.setInfo(task, Optional.of(currentInfo));
					}
				}
			}
			// delete previews
			if (oldHasAsset && !currentHasAsset) {
				deleteNewAssetPreview(folderName);
			}
			
			// update info
			oldFolders.remove(folderName);
			newAssetFolders.put(folderName, currentInfo);
		};
		
		foundNewAssetFolders.forEach(applyEntry);
		
		// removed assets
		for (final AssetName notFound : oldFolders) {
			final AssetTask<?> task = assetTasks.get(notFound);
			if (task != null && task.getAssetProperties(type) != null) {
				taskUpdater.new OnNewAssetUpdate(() -> {
					data.editBy(task, User.UNKNOWN);
				}).removePropsAndSetInfo(task, Optional.empty());
			}
			newAssetFolders.remove(notFound);
			newAssetFoldersWithoutTasks.remove(notFound);
			deleteNewAssetPreview(notFound);
		}
	}
	
	private void assertTaskHasAssetProperties(AssetTask<?> task, AssetGroupType type) {
		final AssetProperties props = task.getAssetProperties(type);
		if (props == null) {
			throw new IllegalStateException("Failed updating asset properties for asset " + task.getAssetName() + "!\n"
					+ "Task " + task.toString() + " exists and should have had asset properties already, but does not!");
		}
	}
	
	private void deleteNewAssetPreview(AssetName name) {
		serviceFinder.getAssetService(name).deleteNewAssetPreview(name);
	}
	
	final DataConfigService config;
	final FileService fileService;
	
	public void deleteAssetFile(AssetName assetFolderName) {
		deleteFile(assetFolderName, FileType.ASSET, null);
	}
	
		
	public void deleteFile(AssetName assetFolderName, FileType fileType, Path relativeFile) {
		final NewAssetFolderInfo folderInfo = getNewAssetFolderInfo(assetFolderName);
		if (!folderInfo.getStatus().isValid()) {
			throw new IllegalArgumentException("Asset folder for given asset deletion is in invalid state!");
		}
		
		final Path absoluteFile;
		
		if (fileType.isAsset()) {
			if (folderInfo.getStatus() != AssetFolderStatus.VALID_WITH_ASSET) {
				throw new IllegalArgumentException("Asset to delete does not exist!");
			}
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
			final Optional<NewAssetFolderInfo> newInfo = scanner.onSingleNewAssetRemoved(folderInfo.getAssetFolder());
			
			final AssetTask<?> task = assetTasks.get(assetFolderName);
			Objects.requireNonNull(task);
			
			if (newInfo.isPresent()) {
				newAssetFolders.put(assetFolderName, newInfo.get());
			} else {
				newAssetFolders.remove(assetFolderName);
				deleteNewAssetPreview(assetFolderName);
			}
			taskUpdater.new OnNewAssetUpdate(() -> {
				data.edit(task);
			}).removePropsAndSetInfo(task, newInfo);
		}
	}
	
	private void addFile(AssetName assetFolderName, FileType fileType, Path relativeFile) {
		
		final NewAssetFolderInfo folderInfo = getNewAssetFolderInfo(assetFolderName);
		Assert.isTrue(folderInfo.getStatus().isValid(), "");
		
		final Path absoluteFile;
		
		if (fileType.isAsset()) {
			if (folderInfo.getStatus() == AssetFolderStatus.VALID_WITH_ASSET) {
				// TODO delete old asset (otherwise 2 assets with same name case-insensitive could exist)
				absoluteFile = folderInfo.getAssetFilePathAbsolute(config);
			}
		} else {
			final Path assetFolder = config.assetsNew().resolve(folderInfo.getAssetFolder());
			final Path visible = assetFolder.resolve(fileType.getSubPath(config));
			absoluteFile = visible.resolve(fileService.restrictAccess(relativeFile, visible));
		}
		
		// TODO decide where to implement this, since its already half-implemented in AssetTaskService, same goes for deleteFile
	}
}
