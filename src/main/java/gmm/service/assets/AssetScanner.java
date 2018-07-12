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
import java.util.Optional;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gmm.collections.ArrayList;
import gmm.collections.List;
import gmm.domain.task.asset.AssetName;
import gmm.service.FileService.FileExtensionFilter;
import gmm.service.data.PathConfig;
import gmm.service.tasks.AssetTaskService;
import gmm.service.tasks.TaskServiceFinder;

@Service
public class AssetScanner {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private final static boolean assetTypeFoldersEnabled = true;
	
	private final PathConfig config;
	private final TaskServiceFinder serviceFinder;
	
	public AssetScanner(PathConfig config, TaskServiceFinder serviceFinder) {
		this.config = config;
		this.serviceFinder = serviceFinder;
	}
	
	/**
	 * Traverses original asset base folder and finds all original assets.
	 */
	public Map<AssetName, OriginalAssetFileInfo> onOriginalAssetFilesChanged() {
		final Map<AssetName, OriginalAssetFileInfo> foundOriginalAssetFiles = new HashMap<>();
		
		final BiConsumer<AssetName, OriginalAssetFileInfo> onHit = (fileName, fileInfo) -> {
			foundOriginalAssetFiles.put(fileName, fileInfo);
			logger.debug("Found original asset file at path '" + fileInfo.getAssetFile() + "'.");
		};
		scanForOriginalAssets(config.assetsOriginal(), onHit);
		return foundOriginalAssetFiles;
	}
	
	private void scanForOriginalAssets(Path originalAssets, BiConsumer<AssetName, OriginalAssetFileInfo> onHit) {
		logger.info("Scanning for original assets at path '" + originalAssets + "'.");
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
	
	/**
	 * Traverses all asset type folders if enabled (otherwise new asset base folder) and finds all
	 * new asset folders.
	 */
	public Map<AssetName, NewAssetFolderInfo> onNewAssetFilesChanged() {
		final Map<AssetName, NewAssetFolderInfo> foundNewAssetFolders = new HashMap<>();
		
		final BiConsumer<AssetName, NewAssetFolderInfo> duplicateCheck = (folderName, folderInfo) -> {
			final NewAssetFolderInfo duplicate = foundNewAssetFolders.get(folderName);
			final NewAssetFolderInfo result;
			if (duplicate != null) {
				result = NewAssetFolderInfo.createInvalidNotUnique(duplicate, folderInfo);
			} else {
				result = folderInfo;
			}
			foundNewAssetFolders.put(folderName, result);
			logger.debug("Found new asset folder at path '" + folderInfo.getAssetFolder() + "'. "
					+ "Status: '" + result.getStatus().name() + "'");
		};
		
		if (assetTypeFoldersEnabled) {
			final Map<Path, AssetTaskService<?>> assetTypeFolders = getNewAssetTypeFolders();
			for (final Entry<Path, AssetTaskService<?>> entry : assetTypeFolders.entrySet()) {
				final Path assetTypeFolder = entry.getKey();
				final AssetTaskService<?> service = entry.getValue();
				scanForNewAssets(assetTypeFolder, duplicateCheck, service);
			}
		} else {
			scanForNewAssets(config.assetsNew(), duplicateCheck, null);
		}
		
		return foundNewAssetFolders;
	}
	
	public Optional<NewAssetFolderInfo> onSingleNewAssetRemoved(Path assetFolderPath) {
		final NewAssetFolderInfo[] foundNewAssetFolder = new NewAssetFolderInfo[1];
		AssetTaskService<?> service = null;
		final Path absoluteFolderPath = config.assetsNew().resolve(assetFolderPath);
		if (assetTypeFoldersEnabled) {
			for (final Entry<Path, AssetTaskService<?>> entry : getNewAssetTypeFolders().entrySet()) {
				if (absoluteFolderPath.startsWith(entry.getKey())) service = entry.getValue();
			}
			if (service == null) throw new IllegalArgumentException("Removed asset can not have been outside an assetTypeFolder!");
		}
		scanForNewAssets(absoluteFolderPath, (folderName, folderInfo) -> {
			foundNewAssetFolder[0] = folderInfo;
		}, service);
		return Optional.ofNullable(foundNewAssetFolder[0]);
	}
	
	private Map<Path, AssetTaskService<?>> getNewAssetTypeFolders() {
		
		final Map<Path, AssetTaskService<?>> result = new HashMap<>();
		
		for (final AssetTaskService<?> service : serviceFinder.getAssetTaskServices()) {
			
			final Path assetTypeFolder = config.assetsNew().resolve(service.getAssetTypeSubFolder());
			final String taskTypeName = service.getTaskType().name();
			
			if (!assetTypeFolder.toFile().isDirectory()) {
				logger.warn("Could not find asset folder for type '" + taskTypeName + "' at '" + assetTypeFolder + "'!");
			} else {
				logger.info("Found asset type folder for type '" + taskTypeName + "' at '" + assetTypeFolder + "'.");
				
				result.put(assetTypeFolder, service);
			}
		}
		return result;
	}
	
	private void scanForNewAssets(Path rootScanFolder, BiConsumer<AssetName, NewAssetFolderInfo> onHit,
			AssetTaskService<?> assetTypeEnabledService) {

		if (!rootScanFolder.toFile().isDirectory()) {
			throw new IllegalArgumentException("Directory expected!");
		}
		logger.info("Scanning for new assets at path '" + rootScanFolder + "'.");
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
	
	/**
	 * Remove any paths from given list that do not qualify as new asset file. Does not guarantee
	 * that all remaining asset paths are valid or exist.
	 * @param pathsRelative - The list that should be modified which contains any paths relative to 
	 * 		{@link PathConfig#assetsNew()}.
	 */
	public void filterForNewAssets(List<Path> pathsRelative) {
		final List<Path> toRemove = new ArrayList<>(Path.class, pathsRelative.size());
		for (final Path path : pathsRelative) {
			final Path folder = path.getParent();
			if (folder == null) {
				toRemove.add(path);
			} else {
				final String folderName = folder.getFileName().toString();
				final String extension = isAssetFolderByConvention(folderName);
				if (extension == null) {
					toRemove.add(path);
				} else {
					final AssetTaskService<?> service = serviceFinder.getAssetService(extension);
					if (service == null) {
						toRemove.add(path);
					}
				}
			}
		}
	}
}
