package gmm.service.assets;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import gmm.domain.User;
import gmm.domain.task.asset.AssetKey;
import gmm.domain.task.asset.AssetName;
import gmm.domain.task.asset.FileType;
import gmm.service.FileService;
import gmm.service.assets.NewAssetFolderInfo.AssetFolderStatus;
import gmm.service.assets.vcs.VcsPlugin;
import gmm.service.data.PathConfig;

@Service
public class NewAssetFileService {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private final AssetService assets;
	private final AssetScanner scanner;
	private final VcsPlugin vcs;
	private final PathConfig config;
	private final FileService fileService;
	
	@Autowired
	public NewAssetFileService(AssetService assets, AssetScanner scanner,
			VcsPlugin vcs, PathConfig config, FileService fileService) {
		this.assets = assets;
		this.scanner = scanner;
		this.vcs = vcs;
		this.config = config;
		this.fileService = fileService;
	}
	
	public void deleteAssetFile(AssetKey assetFolderName, User currentUser) {
		deleteFile(assetFolderName, FileType.ASSET, null, currentUser);
	}

	public void deleteWipFile(AssetKey assetFolderName, Path relativeFile, User currentUser) {
		deleteFile(assetFolderName, FileType.WIP, relativeFile, currentUser);
	}
		
	private void deleteFile(AssetKey assetFolderName, FileType fileType, Path relativeFile, User currentUser) {
		final NewAssetFolderInfo folderInfo = assets.getValidNewAssetFolderInfo(assetFolderName);
		
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
		vcs.removeFile(config.assetsNew().relativize(absoluteFile));
		vcs.commit("GMM: [" + currentUser.getName() +"] deleted " + (fileType.isAsset() ? "an asset." : "a wip file."));
		
		if (fileType.isAsset()) {
			final Optional<NewAssetFolderInfo> oldInfo = Optional.of(folderInfo);
			final Optional<NewAssetFolderInfo> newInfo = scanner.onSingleAssetFolderChanged(folderInfo.getAssetFolder());
			assets.onNewAssetFolderChanged(assetFolderName, oldInfo, newInfo, currentUser);
		} else {
			assets.onNewAssetWipChange(assetFolderName, currentUser);
		}
	}
	
	public void createNewAssetFolder(AssetKey assetFolderName, Path relativeFolder, User currentUser) {
		final NewAssetFolderInfo folderInfo = assets.getNewAssetFolderInfo(assetFolderName);
		if (folderInfo != null) {
			throw new IllegalArgumentException("Asset is already associated with an existing asset folder!");
		}
		final Path absoluteFolder = config.assetsNew().resolve(relativeFolder);
		if (Files.exists(absoluteFolder)) {
			throw new IllegalArgumentException("Path at '" + relativeFolder + "' already exists!");
		}
		logger.info("Creating new asset folder at '" + absoluteFolder + "'");
		fileService.createDirectory(absoluteFolder);
		vcs.addFile(relativeFolder);
		vcs.commit("GMM: [" + currentUser.getName() +"] created asset folder.");
		
		final Optional<NewAssetFolderInfo> oldInfo = Optional.ofNullable(folderInfo);
		final Optional<NewAssetFolderInfo> newInfo = scanner.onSingleAssetFolderChanged(relativeFolder);
		if (!newInfo.isPresent()) {
			throw new IllegalStateException("Scan failed to find newly created asset folder!");
		}
		assets.onNewAssetFolderChanged(assetFolderName, oldInfo, newInfo, currentUser);
	}
	
	public void addWipFile(AssetKey assetName, MultipartFile fileData, User currentUser) {
		
		final NewAssetFolderInfo folderInfo = assets.getValidNewAssetFolderInfo(assetName);
		final String fileName = fileData.getOriginalFilename();
		
		// remove wip file if exists
		final Path folderRelative = folderInfo.getAssetFolder().resolve(config.subOther());
		final Path fileRelative = folderRelative.resolve(fileName);
		final Path fileAbsolute = config.assetsNew().resolve(fileRelative);
		if (Files.exists(fileAbsolute)) {
			if (Files.isRegularFile(fileAbsolute)) {
				throw new IllegalArgumentException("Cannot overwrite directory with file!");
			} else {
				deleteFile(fileRelative, FileType.WIP);
			}
		}
		// add wip file
		{
			createFile(fileRelative, FileType.WIP, fileData);
		}
		vcs.commit("GMM: [" + currentUser.getName() +"] uploaded new wip file.");
		
		assets.onNewAssetWipChange(assetName, currentUser);
	}
	
	public void addAssetFile(AssetKey assetName, MultipartFile fileData, User currentUser) {
		
		final NewAssetFolderInfo oldFolderInfo = assets.getValidNewAssetFolderInfo(assetName);
		final String fileName = fileData.getOriginalFilename();
		final AssetName newAssetName = new AssetName(fileName);
		newAssetName.assertPathMatch(oldFolderInfo.getAssetFolder());
		
		// remove old asset
		if (oldFolderInfo.getStatus() == AssetFolderStatus.VALID_WITH_ASSET) {
			final Path oldAsset = oldFolderInfo.getAssetFilePath(config);
			deleteFile(oldAsset, FileType.ASSET);
		}
		// add new asset
		{
			final Path newAsset = oldFolderInfo.getAssetFolder().resolve(newAssetName.get());
			createFile(newAsset, FileType.ASSET, fileData);
		}
		vcs.commit("GMM: [" + currentUser.getName() +"] uploaded new asset.");
		
		// rescan to get updated folder info
		final Optional<NewAssetFolderInfo> oldInfo = Optional.of(oldFolderInfo);
		final Optional<NewAssetFolderInfo> newInfo = scanner.onSingleAssetFolderChanged(oldFolderInfo.getAssetFolder());
		if (!newInfo.isPresent()) {
			throw new IllegalStateException("Scan failed to relocate asset folder!");
		}
		assets.onNewAssetFolderChanged(assetName, oldInfo, newInfo, currentUser);
	}
	
	/** Delete, log and register in VCS.
	 */
	private void deleteFile(Path relative, FileType type) {
		final Path absolute = config.assetsNew().resolve(relative);
		logger.info("Deleting " + (type.isAsset() ? "new asset" : "wip file") + " at '" + absolute + "'");
		fileService.delete(absolute);
		vcs.removeFile(relative);
	}
	
	/** Create, log and register in VCS.
	 */
	private void createFile(Path relative, FileType type, MultipartFile fileData) {
		final byte[] data;
		try {
			data = fileData.getBytes();
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
		final Path absolute = config.assetsNew().resolve(relative);
		logger.info("Creating " + (type.isAsset() ? "new asset" : "wip file") + " at '" + absolute + "'");
		fileService.createFile(absolute, data);
		vcs.addFile(relative);
	}
}
