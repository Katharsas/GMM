package gmm.service.tasks;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

import gmm.domain.User;
import gmm.domain.task.asset.AssetGroupType;
import gmm.domain.task.asset.AssetName;
import gmm.domain.task.asset.AssetProperties;
import gmm.domain.task.asset.AssetTask;
import gmm.service.FileService;
import gmm.service.FileService.FileExtensionFilter;
import gmm.service.assets.AssetInfo;
import gmm.service.assets.NewAssetFolderInfo;
import gmm.service.assets.NewAssetFolderInfo.AssetFolderStatus;
import gmm.service.assets.OriginalAssetFileInfo;
import gmm.service.data.DataConfigService;
import gmm.web.forms.TaskForm;

/**
 * 
 * @author Jan Mothes
 */
public abstract class AssetTaskService<A extends AssetProperties> extends TaskFormService<AssetTask<A>>{
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired private DataConfigService config;
	@Autowired private FileService fileService;
	
	protected abstract A newPropertyInstance();
	
	protected abstract CompletableFuture<A> recreatePreview(Path sourceFile, Path previewFolder, AssetGroupType type, A props);
	protected abstract void deletePreview(Path previewFolder, AssetGroupType isOriginal);
	protected abstract boolean hasPreview(Path previewFolder, AssetGroupType isOriginal);
	
	protected abstract AssetTask<A> newInstance(AssetName assetName, User user);
	protected abstract String[] getExtensions();
	public abstract Path getAssetTypeSubFolder();
	
	private final FileExtensionFilter extensionFilter;
	
	public AssetTaskService() {
		extensionFilter = new FileExtensionFilter(getExtensions());
	}
	
	@Override
	public final AssetTask<A> create(TaskForm form, User user) {
		final AssetName assetName = new AssetName(getAssetName(form));
		final AssetTask<A> task = newInstance(assetName, user);
		edit(task, form);
		return task;
	}
	
	private String getAssetName(TaskForm form) {
		final String assetName = form.getAssetName();
		if(assetName.equals("")) {
			throw new IllegalStateException("Form does not contain any asset name. Cannot create asset task!");
		}
		return assetName;
	}
	
	@Override
	public void edit(AssetTask<A> task, TaskForm form) {
		super.edit(task, form);
		final AssetName newAssetName = new AssetName(getAssetName(form));
		if(!newAssetName.equals(task.getAssetName())) {
			throw new IllegalArgumentException("Asset path cannot be edited!");
			// TODO if caller makes AssetNameConflictCheck and uses AssetService to relink assets, asset name editing could be enabled.
		}
		//Substitute wildcards
		task.setName(form.getName().replace("%filename%", newAssetName.get()));
		task.setDetails(form.getDetails().replace("%filename%", newAssetName.get()));
	}
	
	@Override
	public TaskForm prepareForm(AssetTask<A> task) {
		final TaskForm form = super.prepareForm(task);
		form.setAssetName(task.getAssetName().toString());
		return form;
	}
	
	public boolean isValidAssetProperties(AssetProperties props, AssetInfo info) {
		
		final File assetFile = getRestrictedAssetPathAbsolute(info.getType(), info).toFile();
		
		if (assetFile.lastModified() != props.getLastModified()) {
			return false;
		}
		if (assetFile.length() != props.getSizeInBytes()) {
			return false;
		}
		// TODO hashcode
		return hasPreview(getPreviewFolder(info.getAssetFileName()), info.getType());
	}
	
	/**
	 * Asynchronously create new preview, create properties from preview, set them on task, set asset info on task.
	 */
	public CompletableFuture<Void> recreateAssetProperties(AssetTask<A> task, AssetInfo info) {
		Objects.requireNonNull(task);
		Objects.requireNonNull(info);
		if (!info.getType().isOriginal()) {
			if (((NewAssetFolderInfo)info).getStatus() != AssetFolderStatus.VALID_WITH_ASSET) {
				throw new IllegalArgumentException("Invalid asset folder state, cannot recreate preview!");
			}
		}
		
		final AssetGroupType type = info.getType();
		final Path assetPathAbs = getRestrictedAssetPathAbsolute(type, info);
		
		final A assetProps = newPropertyInstance();
		assetProps.setSizeInBytes(assetPathAbs.toFile().length());
		assetProps.setLastModified(assetPathAbs.toFile().lastModified());
		
		final Path previewFolder = getPreviewFolder(info.getAssetFileName());
		final CompletableFuture<A> future = recreatePreview(assetPathAbs, previewFolder, type, assetProps);
		
		return future.thenAccept(completedAssetProps -> {
			logger.debug("Set properties & storage info of asset file '" + task.getAssetName() + "' on task '" + task + "'. Type: '" + type.name() + "'");
			if (type.isOriginal()) {
				task.setOriginalAsset(completedAssetProps, (OriginalAssetFileInfo) info);
			} else {
				task.setNewAsset(completedAssetProps, (NewAssetFolderInfo) info);
			}
		});
	}
	
	/**
	 * Remove existing properties from task, set null/invalid info, delete preview.
	 */
	public void removeNewAssetProperties(AssetTask<A> task, Optional<NewAssetFolderInfo> info) {
		final AssetGroupType type = AssetGroupType.NEW;
		if (task.getAssetProperties(type) == null) {
			throw new IllegalArgumentException("Cannot remove properties that don't exist!");
		}
		task.setNewAsset(null, info.orElse(null));
		final Path previewFolder = getPreviewFolder(task.getAssetName());
		deletePreview(previewFolder, type);
		logger.debug("Removed properties & changed storage info of new asset file '" + task.getAssetName() + "' from task '" + task + "'.");
	}
	
	/**
	 * Remove existing properties & info from task, delete preview.
	 */
	public void removeOriginalAssetProperties(AssetTask<A> task) {
		final AssetGroupType type = AssetGroupType.ORIGINAL;
		if (task.getAssetProperties(type) == null) {
			throw new IllegalArgumentException("Cannot remove properties that don't exist!");
		}
		task.setOriginalAsset(null, null);
		final Path previewFolder = getPreviewFolder(task.getAssetName());
		deletePreview(previewFolder, type);
		logger.debug("Removed properties & storage info of original asset file '" + task.getAssetName() + "' from task '" + task + "'.");
	}
	
	private Path getRestrictedAssetPathAbsolute(AssetGroupType type, AssetInfo info) {
		final Path base = config.assetsBase(type);
		final Path assetPathAbs = base.resolve(info.getAssetFilePathAbsolute(config));
		fileService.restrictAccess(assetPathAbs, base);
		return assetPathAbs;
	}
	
	private Path getPreviewFolder(AssetName assetName) {
		return config.assetPreviews().resolve(assetName.getKey());
	}
	
	public void changeNewAssetInfo(AssetTask<A> task, Optional<NewAssetFolderInfo> info) {
		task.setNewAssetFolderInfo(info.orElse(null));
		logger.debug("Changed storage info of new asset file '" + task.getAssetName() + "' for task '" + task + "'.");
	}
	
	public void changeOriginalAssetInfo(AssetTask<A> task, OriginalAssetFileInfo info) {
		task.setOriginalAssetFileInfo(info);
		logger.debug("Changed storage info of original asset file '" + task.getAssetName() + "' for task '" + task + "'.");
	}
	
	// TODO caller must check if the linked task has a new asset folder, otherwise it must ask the user to specify asset folder path
	// TODO if saving asset, caller must delete the old asset first because this func does not know the name
	// of the old asset, every letter may be in different case than new asset filename.
	public void addFile(MultipartFile file, AssetTask<A> task, NewAssetFolderInfo folderInfo) {
		final String fileName = file.getOriginalFilename();
		final boolean isAsset = getExtensionFilter().test(fileName)
				&& new AssetName(fileName).equals(folderInfo.getAssetFileName());
		//Add file
		if (isAsset && folderInfo.getStatus() != AssetFolderStatus.VALID_WITH_ASSET) {
			throw new IllegalStateException("Cannot delete old asset before saving new asset (may have different filename case)!");
		}

		final Path assetFolder = config.assetsNew().resolve(folderInfo.getAssetFolder());
		final Path filePath = assetFolder
				.resolve(isAsset ? config.subAssets() : config.subOther())
				.resolve(fileName);
		try {
			fileService.createFile(filePath, file.getBytes());
		} catch (final IOException e) {
			throw new UncheckedIOException("Could not retrieve bytes from uploaded file '"+fileName+"'!", e);
		}
		// TODO caller must recreate asset properties if this was an asset
		// TODO since caller must know if file was an asset, relay that info to this function or split it into add asset/wip file
	}
	
	public FileExtensionFilter getExtensionFilter() {
		return extensionFilter;
	}
}
