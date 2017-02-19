package gmm.service.assets;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import gmm.collections.HashSet;
import gmm.domain.task.asset.AssetName;
import gmm.service.data.DataConfigService;
import gmm.service.tasks.AssetTaskService;

public class NewAssetFolderInfo {
	
	public static enum AssetFolderStatus {
		
		INVALID_ASSET_FOLDER_NOT_UNIQUE(false),
		INVALID_ASSET_FOLDER_EXTENSION(false),
		INVALID_ASSET_FOLDER_CONTENT(false),
		INVALID_ASSET_FILE_NAME(false),
		
		VALID_WITH_ASSET(true),
		VALID_NO_ASSET(true);
		
		public final boolean isValid;
		AssetFolderStatus(boolean isValid) {
			this.isValid = isValid;
		}
	}
	
	public static NewAssetFolderInfo createInvalidNotUnique(NewAssetFolderInfo duplicate, NewAssetFolderInfo current) {
		return new NewAssetFolderInfo(duplicate, current);
	}
	
	private final AssetTaskService<?> service;
	
	private final Path assetFolder;
	private final AssetName assetFolderName;
	private final AssetName assetFileName;
	
	private final AssetFolderStatus status;
	
	private final HashSet<Path> nonUniqueDuplicates;
	
	private NewAssetFolderInfo(NewAssetFolderInfo duplicate, NewAssetFolderInfo current) {
		this.assetFolder = null;
		this.assetFileName = null;
		this.service = null;
		
		this.assetFolderName = current.assetFolderName;
		
		final AssetFolderStatus notUnique = AssetFolderStatus.INVALID_ASSET_FOLDER_NOT_UNIQUE;
		this.status = notUnique;
		this.nonUniqueDuplicates = new HashSet<>(Path.class);
		
		if (duplicate.status == notUnique) {
			this.nonUniqueDuplicates.addAll(duplicate.nonUniqueDuplicates);
		} else {
			this.nonUniqueDuplicates.add(duplicate.assetFolder);
		}
		
		if (current.status == notUnique) {
			this.nonUniqueDuplicates.addAll(current.nonUniqueDuplicates);
		} else {
			this.nonUniqueDuplicates.add(current.assetFolder);
		}
	}
	
	/**
	 * Use this constructor if no service could be found for this asset folder name to make this 
	 * asset folder invalid.
	 * @param base - absolute root folder for new assets as from {@link DataConfigService#assetsNew()}.
	 * @param relative - path to asset folder relative to base (may include assetTypeFolder).
	 */
	public NewAssetFolderInfo(Path relative) {
		this.nonUniqueDuplicates = null;
		this.service = null;
		this.assetFolder = relative;
		this.assetFolderName = new AssetName(relative);
		this.assetFileName = null;
		status = AssetFolderStatus.INVALID_ASSET_FOLDER_EXTENSION;
	}
	
	/**
	 * @param service - The service that is responsible for this asset folder. If assetTypeFolders
	 * 		is enabled, it may not match the given asset folder name (which makes the folder invalid),
	 * 		because it is the service for all folders inside the assetTypeFolder.
	 * @param base - absolute root folder for new assets as from {@link DataConfigService#assetsNew()}.
	 * @param relative - path to asset folder relative to base (may include assetTypeFolder).
	 */
	public NewAssetFolderInfo(AssetTaskService<?> service, Path relative, Path base) {
		this.nonUniqueDuplicates = null;
		this.service = service;
		this.assetFolder = relative;
		this.assetFolderName = new AssetName(relative);
		
		final Path assetFolderAbs = base.resolve(relative);
		
		final boolean isValidAssetFolderName = service.getExtensionFilter().test(assetFolderName);
		
		AssetName assetFileName = null;
		
		if (!isValidAssetFolderName) {
			status = AssetFolderStatus.INVALID_ASSET_FOLDER_EXTENSION;
		} else {
			final List<Path> files;
			try {
				files = Files.list(assetFolderAbs)
					.filter(path -> path.toFile().isFile())
					.collect(Collectors.toList());
				
			} catch (final IOException e) {
				throw new UncheckedIOException(e);
			}
			
			if (files.size() > 1) {
				status = AssetFolderStatus.INVALID_ASSET_FOLDER_CONTENT;
			} else if (files.size() == 0) {
				status = AssetFolderStatus.VALID_NO_ASSET;
			} else {
				final Path assetFile = files.iterator().next();
				assetFileName = new AssetName(assetFile);
				
				if (!assetFolderName.equals(assetFileName)) {
					status = AssetFolderStatus.INVALID_ASSET_FILE_NAME;
				} else  {
					status = AssetFolderStatus.VALID_WITH_ASSET;
				}
			}
		}
		this.assetFileName = assetFileName;
	}
	
	public AssetFolderStatus getStatus() {
		return status;
	}
	
	/**
	 * @return may be null for status INVALID_ASSET_FOLDER_NOT_UNIQUE, INVALID_ASSET_FOLDER_EXTENSION
	 */
	public Path getAssetFolder() {
		return assetFolder;
	}
	
	
	/**
	 * @return never null.
	 */
	public AssetName getAssetFolderName() {
		return assetFolderName;
	}
	
	/**
	 * @return may be null for most invalid statuses.
	 */
	public AssetName getAssetFileName() {
		return assetFileName;
	}
	
	/**
	 * @return may be null for status INVALID_ASSET_FOLDER_NOT_UNIQUE, INVALID_ASSET_FOLDER_EXTENSION
	 */
	public AssetTaskService<?> getService() {
		return service;
	}
}