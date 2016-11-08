package gmm.service.data;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gmm.domain.task.asset.AssetTypeService;
import gmm.service.tasks.AssetTaskService;

@Service
public class VCSService {
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	public DataConfigService config;
	
	@Autowired
	private AssetTypeService assetTypeService;

	public static enum AssetFolderStatus {
		
		INVALID_ASSET_FOLDER_NAME(false),
		INVALID_ASSET_FOLDER_CONTENT(false),
		INVALID_ASSET_FILE_NAME(false),
		
		VALID_WITH_ASSET(true),
		VALID_NO_ASSET(true);
		
		public final boolean isValid;
		AssetFolderStatus(boolean isValid) {
			this.isValid = isValid;
		}
	}
	
	public class AssetFolder {
//		private final Path assetFolder;
//		private final String assetFileName;
		
		private final AssetFolderStatus status;
		
		/**
		 * @param assetTypeFolder - relative to VCS root folder, must exist
		 * @param assetFolder - relative to assetTypeFolder, must exist
		 */
		public AssetFolder(AssetTaskService<?> service, Path assetFolderAbs, Path assetFolder, String assetFolderName) {
			
			final boolean isValidAssetFolderName = service.getExtensions().test(assetFolderName);
			
			if (!isValidAssetFolderName) {
				status = AssetFolderStatus.INVALID_ASSET_FOLDER_NAME;
			} else {
				final List<Path> files;
				try {
					files = Files.list(assetFolderAbs)
						.filter(path -> path.toFile().isFile())
						.collect(Collectors.toList());
					
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
				if (files.size() > 1) {
					status = AssetFolderStatus.INVALID_ASSET_FILE_NAME;
				} else if (files.size() == 0) {
					status = AssetFolderStatus.VALID_NO_ASSET;
				} else {
					Path assetFile = files.iterator().next();
					if (!assetFolderName.equalsIgnoreCase(assetFile.toFile().getName())) {
						status = AssetFolderStatus.INVALID_ASSET_FILE_NAME;
					} else  {
						status = AssetFolderStatus.VALID_WITH_ASSET;
					}
				}
			}
			
			// TODO which data to should this object hold?
			
			if (status.isValid) {
				
			} else {
				
			}
		}
	}
	
	public Map<String, AssetFolder> scanVCSDirectory() {
		
		for (AssetTaskService<?> service : assetTypeService.getAssetTaskServices()) {
			final Path assetTypeFolder = config.assetsNew().resolve(service.getAssetTypeSubFolder());
			if (assetTypeFolder.toFile().isDirectory()) {
				logger.info("Scanning asset type folder '" + assetTypeFolder + "' for assets.");
				try {
					Files.walkFileTree(assetTypeFolder, new SimpleFileVisitor<Path>() {
						@Override
						public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
							final String dirName = dir.toFile().getName();
							final String[] dirNameParts = dirName.split(".");
							// if dir name has minimum of two parts (name, extension) separated by '.'
							if (dirNameParts.length >= 2) {
								final Path assetFolder = assetTypeFolder.relativize(dir);
								final AssetFolder folderInfo
										= new AssetFolder(service, dir, assetFolder, dirName);
								
								// TODO put folderInfo into map ?
								// TODO should AssetFolder contain Service reference ? this would AsseTasks
								// when linked to directly use that service or hold it for later (previews?)
								
								
								return FileVisitResult.SKIP_SUBTREE;
							}
							return FileVisitResult.CONTINUE;
						}
					});
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			} else {
				logger.warn("Could not find asset folder at '" + assetTypeFolder + "'!");
			}
		}
		
		return null;
	}
	
//	public static class AssetFolderVisitor extends SimpleFileVisitor<Path> {
//		
//		private final AssetTaskService<?> service;
//		private final Path assetTypeFolderAbs;
//		
//		public AssetFolderVisitor(AssetTaskService<?> service, Path assetTypeFolderAbs) {
//			this.service = service;
//			this.assetTypeFolderAbs = assetTypeFolderAbs;
//		}
//		
//		
//	}
}
