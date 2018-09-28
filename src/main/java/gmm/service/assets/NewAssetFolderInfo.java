package gmm.service.assets;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import gmm.collections.HashSet;
import gmm.collections.Set;
import gmm.domain.task.asset.AssetGroupType;
import gmm.domain.task.asset.AssetName;
import gmm.service.data.PathConfig;
import gmm.service.tasks.AssetTaskService;

/**
 * Immutable.
 * 
 * @author Jan Mothes
 */
public class NewAssetFolderInfo implements AssetInfo {
	
	public static enum AssetFolderStatus {
		
		INVALID_ASSET_FOLDER_NOT_UNIQUE(false, "folder.duplicates"),
		INVALID_ASSET_FOLDER_EXTENSION(false, "folder.extension"),
		INVALID_ASSET_FOLDER_CONTENT(false, "folder.content"),
		INVALID_ASSET_FILE_NAME(false, "folder.filename"),
		
		VALID_WITH_ASSET(true, null),
		VALID_NO_ASSET(true, "noasset");
		
		private final boolean isValid;
		private final String messageKey;
		
		AssetFolderStatus(boolean isValid, String messageKey) {
			this.isValid = isValid;
			if (messageKey == null) this.messageKey = null;
			else {
				this.messageKey = "assets.new." + (isValid ? "valid." : "invalid.") + messageKey;
			}
		}
		public String getMessageKey() {
			return messageKey;
		}
		public boolean isValid() {
			return isValid;
		}
		public boolean hasAsset() {
			return this == VALID_WITH_ASSET;
		}
	}
	
	public static NewAssetFolderInfo createInvalidNotUnique(NewAssetFolderInfo duplicate, NewAssetFolderInfo current) {
		return new NewAssetFolderInfo(duplicate, current);
	}
	
	@XStreamAsAttribute
	private final Path assetFolder;
	@XStreamAsAttribute
	private final AssetName assetFolderName;
	@XStreamAsAttribute
	private final AssetName assetFileName;
	
	@XStreamAsAttribute
	private final AssetFolderStatus status;
	
	private final HashSet<Path> nonUniqueDuplicates;
	
	private NewAssetFolderInfo(NewAssetFolderInfo duplicate, NewAssetFolderInfo current) {
		this.assetFolder = null;
		this.assetFileName = null;
		
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
	 * @param base - absolute root folder for new assets as from {@link PathConfig#assetsNew()}.
	 * @param relative - path to asset folder relative to base (may include assetTypeFolder).
	 */
	public NewAssetFolderInfo(Path relative) {
		this.nonUniqueDuplicates = null;
		this.assetFolder = relative;
		this.assetFolderName = new AssetName(relative);
		this.assetFileName = null;
		status = AssetFolderStatus.INVALID_ASSET_FOLDER_EXTENSION;
	}
	
	/**
	 * @param service - The service that is responsible for this asset folder. If assetTypeFolders
	 * 		is enabled, it may not match the given asset folder name (which makes the folder invalid),
	 * 		because it is the service for all folders inside the assetTypeFolder.
	 * @param base - absolute root folder for new assets as from {@link PathConfig#assetsNew()}.
	 * @param relative - path to asset folder relative to base (may include assetTypeFolder).
	 */
	public NewAssetFolderInfo(AssetTaskService<?> service, Path relative, Path base) {
		this.nonUniqueDuplicates = null;
		this.assetFolder = relative;
		this.assetFolderName = new AssetName(relative);
		
		final Path assetFolderAbs = base.resolve(relative);
		
		final boolean isValidAssetFolderName = service.getExtensionFilter().test(assetFolderName);
		
		AssetName assetFileName = null;
		
		if (!isValidAssetFolderName) {
			status = AssetFolderStatus.INVALID_ASSET_FOLDER_EXTENSION;
		} else {
			final List<Path> files;
			try(Stream<Path> stream = Files.list(assetFolderAbs)) {
				files = stream
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
	
	@Override
	public Path getDisplayPath() {
		return getAssetFolder();
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
	@Override
	public AssetName getAssetFileName() {
		return assetFileName;
	}

	@Override
	public AssetGroupType getType() {
		return AssetGroupType.NEW;
	}
	
	public Path getAssetFilePath(PathConfig config) {
		if (status != AssetFolderStatus.VALID_WITH_ASSET) {
			throw new UnsupportedOperationException("Cannot return path to asset since it does not exist!");
		}
		if (assetFolder == null || assetFileName == null) {
			throw new NullPointerException();
		}
		return assetFolder
				.resolve(config.subAssets())
				.resolve(assetFileName.get());
	}

	@Override
	public Path getAssetFilePathAbsolute(PathConfig config) {
		return config.assetsNew().resolve(getAssetFilePath(config));
	}
	
	public Set<Path> getErrorPaths() {
		if (status == AssetFolderStatus.INVALID_ASSET_FOLDER_NOT_UNIQUE) {
			return nonUniqueDuplicates;
		} else {
			final Set<Path> result = new HashSet<>(Path.class, 1);
			result.add(getAssetFolder());
			return result;
		}
	}

	// TODO check if equals & hashcode are actually called?
	// TODO
	// TODO
	
	@Override
	public int hashCode() {
		return Objects.hash(assetFileName, assetFolder, assetFolderName, nonUniqueDuplicates, status);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final NewAssetFolderInfo other = (NewAssetFolderInfo) obj;
		
		if (!Objects.equals(assetFileName, other.assetFileName)) return false;
		if (!Objects.equals(assetFolder, other.assetFolder)) return false;
		if (!Objects.equals(assetFolderName, other.assetFolderName)) return false;
		if (!Objects.equals(nonUniqueDuplicates, other.nonUniqueDuplicates)) return false;
		if (!Objects.equals(status, other.status)) return false;
		
		return true;
	}
}