package gmm.service.assets;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gmm.collections.HashSet;
import gmm.collections.List;
import gmm.collections.Set;
import gmm.domain.Linkable;
import gmm.domain.task.Task;
import gmm.domain.task.asset.AssetGroupType;
import gmm.domain.task.asset.AssetName;
import gmm.domain.task.asset.AssetProperties;
import gmm.domain.task.asset.AssetTask;
import gmm.service.FileService.FileExtensionFilter;
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
	
	private final static boolean assetTypeFoldersEnabled = true;
	
	private final DataConfigService config;
	private final TaskServiceFinder serviceFinder;
	
	public final Map<AssetName, AssetTask<?>> assetTasks;
	
	public final Map<AssetName, NewAssetFolderInfo> newAssetFolders;
	public final Map<AssetName, OriginalAssetFileInfo> originalAssetFiles;
	
	public DataChangeCallback reference;
	
	public NewAssetFolderInfo getNewAssetFolderInfo(AssetName assetName) {
		return newAssetFolders.get(assetName);
	}
	
	public OriginalAssetFileInfo getOriginalAssetFileInfo(AssetName assetName) {
		return originalAssetFiles.get(assetName);
	}
	
	@Autowired
	public AssetService(DataConfigService config, TaskServiceFinder serviceFinder) {
		this.config = config;
		this.serviceFinder = serviceFinder;
		
		assetTasks = new HashMap<>();
		newAssetFolders = new HashMap<>();
		originalAssetFiles = new HashMap<>();
	}
	
	@Autowired
	public void setDataProvider(DataAccess data) {
		for (final AssetTask<?> assetTask : data.getList(AssetTask.class)) {
			assetTasks.put(assetTask.getAssetName(), assetTask);
		}
		
		reference = new DataChangeCallback() {
			@Override
			public void onEvent(DataChangeEvent event) {
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
		};
		data.registerPostProcessor(reference);
		
//		onOriginalAssetFilesChanged();
		// TODO make sure the order of service initialization works out like this:
		// 1. DataAccess init, ServiceFinder init
		// 2. This method runs
		// 3. AutoBackupLoader runs
	}
	
	@PostConstruct
	public void initialScans() {
		onOriginalAssetFilesChanged();
	}
	
	/**
	 * Not needed for loaded tasks since their original properties dont change
	 */
	private <A extends AssetProperties> void onAssetTaskCreation(AssetTask<A> task) {
		final AssetName name = task.getAssetName();
		assetTasks.put(name, task);
		
		final AssetGroupType type = AssetGroupType.ORIGINAL;
		final OriginalAssetFileInfo info = originalAssetFiles.get(name);
		final AssetTaskService<A> service = serviceFinder.getAssetService(Util.classOf(task));
		
		if (info != null) {
			final AssetProperties props = task.getAssetProperties(type);
			if (props != null && service.isValidAssetProperties(props, info)) {
				return; // we assume the existing prop data and previews are still valid, no need for recreation.
			}
		}
		service.recreateAssetProperties(task, info);
	}
	
	private <A extends AssetProperties> void onAssetTaskDeletion(AssetTask<A> task) {
		final AssetName name = task.getAssetName();
		assetTasks.remove(name);
		
		final AssetGroupType type = AssetGroupType.ORIGINAL;
		if (task.getAssetProperties(type) == null) return;
		else {
			final AssetTaskService<A> service = serviceFinder.getAssetService(Util.classOf(task));
			service.removeAssetProperties(task, AssetGroupType.ORIGINAL);
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onOriginalAssetFilesChanged() {
		
		final Set<AssetName> oldFiles = new HashSet<>(AssetName.class, originalAssetFiles.keySet());
		
		scanForOriginalAssets(config.assetsOriginal(), new BiConsumer<AssetName, OriginalAssetFileInfo>() {
			@Override
			public void accept(AssetName fileName, OriginalAssetFileInfo info) {
				logger.debug("Found original asset file at path '" + info.getAssetFile() + "'.");
				
				final OriginalAssetFileInfo oldInfo = originalAssetFiles.get(fileName);
				final AssetTask<?> task = assetTasks.get(fileName);
				
				if (task != null) {
					final AssetTaskService<?> service =
							serviceFinder.getAssetService(Util.classOf(task));
					if (oldInfo == null) {
						// new assets
						service.recreateAssetProperties((AssetTask)task, info);
					} else {
						// changed assets
						final Path oldPath = oldInfo.getAssetFile();
						final Path newPath = info.getAssetFile();
						if (!oldPath.equals(newPath)) {
//							service.removeAssetProperties((AssetTask)task, AssetGroupType.ORIGINAL);
							service.recreateAssetProperties((AssetTask)task, info);
						}
					}
				}
				oldFiles.remove(fileName);
				originalAssetFiles.put(fileName, info);
			}
		});
		// removed assets
		for (final AssetName notFound : oldFiles) {
			final AssetTask<?> task = assetTasks.get(notFound);
			if (task != null) {
				final AssetTaskService<?> service = serviceFinder.getAssetService(notFound);
				service.removeAssetProperties((AssetTask)task, AssetGroupType.ORIGINAL);
			}
		}
	}
	
	private void scanForOriginalAssets(Path originalAssets, BiConsumer<AssetName, OriginalAssetFileInfo> onHit) {
		logger.debug("Scanning for original assets at path '" + originalAssets + "'.");
		try {
			Files.walkFileTree(originalAssets, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
					final String fileNameString = file.getFileName().toString();
					
					final AssetTaskService<?> service = isOriginalAsset(fileNameString);
					if (service != null) {
						final Path relative =  config.assetsOriginal().relativize(file);
						final OriginalAssetFileInfo info = new OriginalAssetFileInfo(service, relative);
						onHit.accept(info.getAssetFileName(), info);
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	/**
	 * @return The service associated with files of this type if the file is an asset, null otherwise.
	 */
	private AssetTaskService<?> isOriginalAsset(String fileName) {
		final String extension = FileExtensionFilter.getExtension(fileName);
		if (extension == null) return null;
		else return serviceFinder.getAssetService(extension);
	}
	
	// TODO make sure working copy is up to date when this is called
	/**
	 * @param changedPaths - This method does detect some changes, but if a file changed its content
	 * 		without changing is filename, its path should be in the given list.
	 */
	public void onNewAssetFilesChanged(List<Path> changedPaths) {
		final Map<AssetName, NewAssetFolderInfo> newAssetFolders = new HashMap<>();
		
		final BiConsumer<AssetName, NewAssetFolderInfo> onHit = (folderName, folderInfo) -> {
			logger.debug("Found new asset folder at path '" + folderInfo.getAssetFolder() + "'.");
			final NewAssetFolderInfo duplicate = newAssetFolders.get(folderName);
			if (duplicate != null) {
				newAssetFolders.put(folderName, NewAssetFolderInfo.createInvalidNotUnique(duplicate, folderInfo));
			} else {
				newAssetFolders.put(folderName, folderInfo);
			}
		};
		
		if (assetTypeFoldersEnabled) {
			final Map<Path, AssetTaskService<?>> assetTypeFolders = getNewAssetTypeFolders();
			for (final Entry<Path, AssetTaskService<?>> entry : assetTypeFolders.entrySet()) {
				final Path assetTypeFolder = entry.getKey();
				final AssetTaskService<?> service = entry.getValue();
				scanForNewAssets(assetTypeFolder, onHit, service);
			}
		} else {
			scanForNewAssets(config.assetsNew(), onHit, null);
		}
		
		// TODO
		// folder info that didn't exist before got added. Find task, set properties if valid & assetfile exists
		// folder info that wasnt found this time got deleted. Find task and remove any properties if old info was valid & assetfile existed
		// folder info with changed state: if changed from/to valid with asset, delete/create properties for asset
		// folder info valid with asset for both old/new, but folderpath changed: recreate properties or rely on changedPaths (SVN diff)
		// folder info valid with asset for both old/new: rely on changedPaths (SVN diff)
		
	}
	
	private Map<Path, AssetTaskService<?>> getNewAssetTypeFolders() {
		
		final Map<Path, AssetTaskService<?>> result = new HashMap<>();
		
		for (final AssetTaskService<?> service : serviceFinder.getAssetTaskServices()) {
			
			final Path assetTypeFolder = config.assetsNew().resolve(service.getAssetTypeSubFolder());
			final String taskTypeName = service.getTaskType().getSimpleName();
			
			if (!assetTypeFolder.toFile().isDirectory()) {
				logger.warn("Could not find asset folder for type '" + taskTypeName + "' at '" + assetTypeFolder + "'!");
			} else {
				logger.info("Found asset type folder for type '" + taskTypeName + "' at '" + assetTypeFolder + "'.");
				
				result.put(assetTypeFolder, service);
			}
		}
		return result;
	}
	
	public void scanForNewAssets(Path rootScanFolder, BiConsumer<AssetName, NewAssetFolderInfo> onHit,
			AssetTaskService<?> assetTypeEnabledService) {

		if (!rootScanFolder.toFile().isDirectory()) {
			throw new IllegalArgumentException("Directory expected!");
		}
		logger.debug("Scanning for new assets at path '" + rootScanFolder + "'.");
		try {
			Files.walkFileTree(rootScanFolder, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
					
					final String dirNameString = dir.getFileName().toString();
					final String extension = isAssetFolderByConvention(dirNameString);
					if (extension != null) {
						final AssetTaskService<?> service = serviceFinder.getAssetService(extension);
						final Path base = config.assetsNew();
						final Path relative = base.relativize(dir);
						final NewAssetFolderInfo folderInfo;
						if (assetTypeFoldersEnabled) {
							if (service == null) {
								folderInfo = new NewAssetFolderInfo(relative);
							} else {
								folderInfo = new NewAssetFolderInfo(assetTypeEnabledService, relative, base);
							}
						} else {
							if (service == null) {
								return FileVisitResult.SKIP_SUBTREE;
							} else {
								folderInfo = new NewAssetFolderInfo(service, relative, base);
							}
						}
						onHit.accept(folderInfo.getAssetFolderName(), folderInfo);
						return FileVisitResult.SKIP_SUBTREE;
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	/**
	 * @return The extension of the folder name if the folder is an asset folder, otherwise null.
	 */
	private String isAssetFolderByConvention(String folderName) {
		// by convention, an asset folder is named like a file, with file name and file extension
		// separated by a point.
		return FileExtensionFilter.getExtension(folderName);
	}
}
