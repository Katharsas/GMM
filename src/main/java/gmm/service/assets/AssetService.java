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

import gmm.collections.Collection;
import gmm.collections.EventMap;
import gmm.collections.EventMapSource;
import gmm.collections.HashSet;
import gmm.collections.List;
import gmm.collections.Set;
import gmm.collections.UnmodifiableCollection;
import gmm.domain.User;
import gmm.domain.task.asset.AssetGroupType;
import gmm.domain.task.asset.AssetKey;
import gmm.domain.task.asset.AssetProperties;
import gmm.domain.task.asset.AssetTask;
import gmm.service.FileService;
import gmm.service.assets.AssetTaskBatchUpdater.OnAssetUpdateProvider;
import gmm.service.assets.AssetTaskUpdater.OnNewAssetUpdate;
import gmm.service.assets.AssetTaskUpdater.OnOriginalAssetUpdate;
import gmm.service.assets.NewAssetFolderInfo.AssetFolderStatus;
import gmm.service.assets.vcs.VcsPlugin;
import gmm.service.data.DataAccess;
import gmm.service.data.DataChangeEvent;
import gmm.service.data.PathConfig;
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
	
	@SuppressWarnings("unused")
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private final PathConfig config;
	private final AssetScanner scanner;
	private final TaskServiceFinder serviceFinder;
	private final AssetTaskUpdater taskUpdater;
	private final AssetTaskBatchUpdater taskBatchUpdater;
	private final DataAccess data;
	private final NewAssetLockService lockService;

	
	// TODO currently, there is a small preview leak, because new assets are scanned for the first
	// time AFTER VcsPlugin updated the WC. This means that any new assets that were deleted during
	// offline GMM will still have previews left and cannot be found because AssetInfo is missing in
	// new scan. This could be fixed by scanning once more before initializing VcsPlugin. AssetInfo
	// would then be removed together with previews when scanning updated WC.
	
	private final EventMap<AssetKey, AssetTask<?>> assetTasks;
	
	private final EventMap<AssetKey, NewAssetFolderInfo> newAssetFolders;
	private final Map<AssetKey, NewAssetFolderInfo> newAssetFoldersWithoutTasks;
	
	private final Map<AssetKey, OriginalAssetFileInfo> originalAssetFiles;
	
	public NewAssetFolderInfo getNewAssetFolderInfo(AssetKey assetName) {
		return newAssetFolders.get(assetName);
	}
	
	/** @see {@link #getNewAssetFolderInfo(AssetKey)}
	 */
	public NewAssetFolderInfo getValidNewAssetFolderInfo(AssetKey assetName) {
		final NewAssetFolderInfo folderInfo = getNewAssetFolderInfo(assetName);
		if (folderInfo == null) {
			throw new IllegalArgumentException("Asset folder could not be found.");
		}
		final AssetFolderStatus status = folderInfo.getStatus();
		if (!status.isValid()) {
			throw new IllegalArgumentException("Asset folder must be in valid state, but is '" + status.name() + "'.");
		}
		return folderInfo;
	}
	
	/**
	 * @return Live unmodifiable view.
	 */
	public Collection<NewAssetFolderInfo> getNewAssetFoldersWithoutTasks() {
		return new UnmodifiableCollection<>(NewAssetFolderInfo.class, newAssetFoldersWithoutTasks.values());
	}
	
	public OriginalAssetFileInfo getOriginalAssetFileInfo(AssetKey assetName) {
		return originalAssetFiles.get(assetName);
	}
	
	public EventMapSource<AssetKey, NewAssetFolderInfo> getNewAssetFoldersEvents() {
		return newAssetFolders;
	}
	
	public EventMapSource<AssetKey, AssetTask<?>> getNewAssetFoldersTaskEvents() {
		return assetTasks;
	}
	
	@Autowired
	public AssetService(AssetScanner scanner, TaskServiceFinder serviceFinder, AssetTaskUpdater taskUpdater,
			VcsPlugin vcs, PathConfig config, FileService fileService, DataAccess data, 
			NewAssetLockService lockService, AssetTaskBatchUpdater taskBatchUpdater) {
		this.scanner = scanner;
		this.serviceFinder = serviceFinder;
		this.taskUpdater = taskUpdater;
		this.data = data;
		this.lockService = lockService;
		this.taskBatchUpdater = taskBatchUpdater;
		this.config = config;
		
		assetTasks = new EventMap<>(new ConcurrentHashMap<>());
		newAssetFolders = new EventMap<>(new ConcurrentHashMap<>());
		newAssetFoldersWithoutTasks = new ConcurrentHashMap<>();
		originalAssetFiles = new ConcurrentHashMap<>();
		
		fileService.createDirectory(config.assetsNew());
		fileService.createDirectory(config.assetsOriginal());
		
		initTasks(data);
		data.<AssetTask<?>>registerPostProcessor(this::onDataChangeEvent, AssetTask.getGenericClass());
		
		onOriginalAssetFilesChanged();
		vcs.registerFilesChangedHandler(this);
	}
	
	private void initTasks(DataAccess data) {
		for (final AssetTask<?> assetTask : data.getList(AssetTask.class)) {
			assetTasks.put(assetTask.getAssetName().getKey(), assetTask);
		}
	}
	
	private void onDataChangeEvent(DataChangeEvent<? extends AssetTask<?>> event) {
		switch(event.type) {
		case ADDED:
			try {
				// TODO
				// This is the second of a batch of events from DB on init thread. DB locks
				// are held by init thread until all events are through.
				// The first event spawned a threadpool worker which holds newasset-lock and is
				// deadlocked trying to call db because we still lock db while trying to get
				// newasset-lock anew.
				//
				// Solution A:
				// DB lock must be free while any events are fired. but ALSO db must fire
				// each event per type before processing next type (to not break Texture model linking)
				// Solution B:
				// DB must be lock free using concurrent hashmaps
				// Solution C:
				// new asset lock should be shared by threadpools doing read only, so that
				// calling multiple preview generation batches does not require lock stealing.
				// In other words, use ReadWriteLock instead of simple lock.
				// Also, does OScfile locking cause IOException? if it just blocks we can just
				// make any reads not require locks, otherwise all ALL (event preview send in
				// controller) should hold read lock
				//
				// Preferred solution:
				// DB locks for postprocessing make kind of sense. we want to have the preprocessing
				// done before we allow others to call into db. So solution A is not so good.
				// (For normal change handlers on the other hand, it may sometimes and sometines not)
				//
				// => make change handling not require holding db lock
				// 
				// Solution C preferred for this problem though
				// 
				// Anyway:
				// Data transactions must be better defined. Which things in DB must be done
				// atomically? For example TextureModelLinking can edit a texture when model has
				// changed, not implemented as postprocessing, but should be atomic really. If not,
				// any thread can change model or texture while texture is being updated.
				//
				// Also, while previews are generated, we really wan to make sure we either
				// - block write access to tasks for which previews are generated
				// - or allow cancel/restart preview generation (??)
				// For this, more finegrained task blocking is necessary
				
				lockService.readLock("AssetService::onDataChangeEvent");
				taskBatchUpdater.updateTasks(event.changed, this::onAssetTaskCreation, updatedTasks -> {
					data.editAllBy(updatedTasks, User.SYSTEM);
				});
				break;
			} finally {
				lockService.readUnlock("AssetService::onDataChangeEvent");
			}
		case EDITED:
			// TODO AssetService needs to get notified manually on AssetName change,
			// see AssetTaskService todo, AssetService needs old AssetName to cleanup
			break;
		case REMOVED:
			// 
			for (final AssetTask<?> task : event.changed) {
				onAssetTaskDeletion(task);
			}
			break;
		}
	}
	
	/**
	 * Deletes mapping from asset to given task (if any exists).
	 */
	private <A extends AssetProperties> void onAssetTaskDeletion(AssetTask<A> task) {
		final AssetKey name = task.getAssetName().getKey();
		assetTasks.remove(name);
		
		final NewAssetFolderInfo newFolderInfo = newAssetFolders.get(name);
		if (newFolderInfo != null) newAssetFoldersWithoutTasks.put(name, newFolderInfo);
	}
	
	/**
	 * Synchronize an asset task and its property information with existing assets.
	 */
	private <A extends AssetProperties> void onAssetTaskCreation(AssetTask<A> task, OnAssetUpdateProvider updaterProvider) {
		final AssetKey name = task.getAssetName().getKey();
		assetTasks.put(name, task);
		
		final AssetTaskService<A> service = serviceFinder.getAssetService(Util.classOf(task));
		{
			final AssetGroupType type = AssetGroupType.ORIGINAL;
			final OriginalAssetFileInfo info = getOriginalAssetFileInfo(name);
			final AssetProperties props = task.getAssetProperties(type);
			
			final OnOriginalAssetUpdate updater = updaterProvider.createOnOriginalAssetUpdate();
			
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
			
			final OnNewAssetUpdate updater = updaterProvider.createOnNewAssetUpdate();
			
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
	
	private void onOriginalAssetFilesChanged() {
		applyFoundOriginal(scanner.onOriginalAssetFilesChanged());
	}
	
	private void applyFoundOriginal(Map<AssetKey, OriginalAssetFileInfo> foundOriginalAssetFiles) {
		
		final AssetGroupType type = AssetGroupType.ORIGINAL;
		final Set<AssetKey> oldFiles = new HashSet<>(AssetKey.class, originalAssetFiles.keySet());
		
		foundOriginalAssetFiles.forEach((fileName, currentInfo) -> {
			
			final AssetTask<?> task = assetTasks.get(fileName);
			if (task != null) {
				final OriginalAssetFileInfo oldInfo = originalAssetFiles.get(fileName);
				final AssetTaskService<?> service = serviceFinder.getAssetService(Util.classOf(task));
				// TODO use batch updater
				final OnOriginalAssetUpdate update = taskUpdater.new OnOriginalAssetUpdate(result -> {
					data.edit(result);
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
		for (final AssetKey notFound : oldFiles) {
			final AssetTask<?> task = assetTasks.get(notFound);
			if (task != null) {
				// TODO use batch updater
				final OnOriginalAssetUpdate update = taskUpdater.new OnOriginalAssetUpdate(result -> {
					data.edit(result);
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
	
	private void applyFoundNew(Map<AssetKey, NewAssetFolderInfo> foundNewAssetFolders, List<Path> changedPaths) {
		
		scanner.filterForNewAssets(changedPaths);
		
		final AssetGroupType type = AssetGroupType.NEW;
		final Set<AssetKey> oldFolders = new HashSet<>(AssetKey.class, newAssetFolders.keySet());
		
		final BiConsumer<AssetKey, NewAssetFolderInfo> applyEntry = (folderName, currentInfo) -> {
			
			final NewAssetFolderInfo oldInfo = newAssetFolders.get(folderName);
			final AssetFolderStatus hasAsset = AssetFolderStatus.VALID_WITH_ASSET;
			final boolean oldHasAsset = oldInfo != null && oldInfo.getStatus() == hasAsset;
			final boolean currentHasAsset = currentInfo.getStatus() == hasAsset;
			
			// update tasks
			final AssetTask<?> task = assetTasks.get(folderName);
			if (task == null) {
				newAssetFoldersWithoutTasks.put(folderName, currentInfo);
			} else {
				final boolean forceUpdate = oldHasAsset && currentHasAsset
						&& changedPaths.contains(currentInfo.getAssetFilePath(config).normalize());
				// TODO call remove instead of contains?
				
				// TODO use batch updater
				final OnNewAssetUpdate updater = taskUpdater.new OnNewAssetUpdate(result -> {
					data.editBy(result, User.UNKNOWN);
					// TODO calling edit causes the onDataChangeEvent above to be called, so we could just do everything in there
					// (the old info would be available through tasks), same goes for original tasks
				});
				
				updateChangedInfo(oldInfo, currentInfo, updater, task, forceUpdate);
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
		for (final AssetKey notFound : oldFolders) {
			final AssetTask<?> task = assetTasks.get(notFound);
			if (task != null) {
				// TODO use batch updater
				OnNewAssetUpdate updater = taskUpdater.new OnNewAssetUpdate(result -> {
					data.editBy(result, User.UNKNOWN);
				});
				if (task.getAssetProperties(type) != null) {
					updater.removePropsAndSetInfo(task, Optional.empty());
				} else {
					updater.setInfo(task, Optional.empty());
				}
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
	
	/** Deletes if exists.
	 */
	private void deleteNewAssetPreview(AssetKey name) {
		serviceFinder.getAssetService(name).deleteNewAssetPreview(name);
	}
	
	/**
	 * Updates AssetService about changes to wip folder of an asset.
	 */
	public void onNewAssetWipChange(AssetKey asset, User user) {
		getValidNewAssetFolderInfo(asset);
		final AssetTask<?> task = assetTasks.get(asset);
		Objects.requireNonNull(task);
		data.editBy(task, user);
	}
	
	/**
	 * Updates AssetService about single changes to new assets or asset folders.
	 * Can handle any changes that may occur to an asset folder.
	 */
	public void onNewAssetFolderChanged(
			AssetKey asset, 
			Optional<NewAssetFolderInfo> oldInfo,
			Optional<NewAssetFolderInfo> newInfo,
			User user) {
		
		if (!newInfo.isPresent()) {
			if (!oldInfo.isPresent()) {
				throw new IllegalStateException("Asset folder change could not be detected!");
			} else {
				newAssetFolders.remove(asset);
			}
		} else {
			newAssetFolders.put(asset, newInfo.get());
		}
		try {
			lockService.readLock("AssetService::onNewAssetFolderChanged");
			final AssetTask<?> task = assetTasks.get(asset);
			Objects.requireNonNull(task);
			// TODO use batch updater
			final OnNewAssetUpdate updater = taskUpdater.new OnNewAssetUpdate(result -> {
				data.editBy(result, user);
			});
			
			if (oldInfo.isPresent()) {
				if (newInfo.isPresent()) {
					// deal with asset file changes or folder relocation only
					updateChangedInfo(oldInfo.get(), newInfo.get(), updater, task, true);
				} else {
					// asset folder and any assets were deleted
					if (oldInfo.get().getStatus().hasAsset()) {
						updater.removePropsAndSetInfo(task, newInfo);
					} else {
						updater.setInfo(task, newInfo);
					}
				}
			} else {
				if (newInfo.isPresent()) {
					// folder was created, maybe asset was as well
					if (newInfo.get().getStatus().hasAsset()) {
						updater.recreatePropsAndSetInfo(task, newInfo.get());
					} else {
						updater.setInfo(task, newInfo);
					}
				}
			}
		} finally {
			lockService.readUnlock("AssetService::onNewAssetFolderChanged");
		}
		if (isPreviewDeletionNeeded(oldInfo, newInfo)) {
			deleteNewAssetPreview(asset);
		}
	}
	
	private void updateChangedInfo(
			NewAssetFolderInfo oldInfo,
			NewAssetFolderInfo newInfo,
			OnNewAssetUpdate updater,
			AssetTask<?> task,
			boolean forceUpdate) {
		
		final boolean oldHasAsset = oldInfo.getStatus().hasAsset();
		final boolean currentHasAsset = newInfo.getStatus().hasAsset();
		final AssetGroupType type = AssetGroupType.NEW;
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		final AssetTaskService<?> service = serviceFinder.getAssetService((Class)Util.getClass(task));
		
		// asset was removed
		if (oldHasAsset && !currentHasAsset) {
			assertTaskHasAssetProperties(task, type);
			updater.removePropsAndSetInfo(task, Optional.of(newInfo));
		// asset was added
		} else if (!oldHasAsset && currentHasAsset) {
			updater.recreatePropsAndSetInfo(task, newInfo);
		// asset still exists
		} else if (oldHasAsset && currentHasAsset) {
			assertTaskHasAssetProperties(task, type);
			final AssetProperties props = task.getAssetProperties(type);
			// content has changed
			if (forceUpdate) {
				updater.recreatePropsAndSetInfo(task, newInfo);
			}
			// content may have changed
			else if (!service.isValidAssetProperties(props, newInfo)) {
				updater.recreatePropsAndSetInfo(task, newInfo);
			// file path may have changed, content hasn't changed
			} else {
				if (!Objects.equals(oldInfo, newInfo)) {
					updater.setInfo(task, Optional.of(newInfo));
				}
			}
		// asset still does not exist, but folder status may have changed
		} else {
			if (!Objects.equals(oldInfo, newInfo)) {
				updater.setInfo(task, Optional.of(newInfo));
			}
		}
	}
	
	private boolean isPreviewDeletionNeeded(
			Optional<NewAssetFolderInfo> oldInfo,
			Optional<NewAssetFolderInfo> newInfo) {
		
		final boolean oldHasAsset = oldInfo.isPresent() && oldInfo.get().getStatus().hasAsset();
		if (oldHasAsset) {
			if (newInfo.isPresent()) {
				return !newInfo.get().getStatus().hasAsset();
			} else {
				return true;
			}
		} else {
			return false;
		}
	}
}
