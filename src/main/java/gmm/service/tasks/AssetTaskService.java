package gmm.service.tasks;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

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
	
	protected abstract A newPropertyInstance(String filename, AssetGroupType isOriginal);
	
	protected abstract void recreatePreview(Path sourceFile, Path previewFolder, A props);
	protected abstract void deletePreview(Path previewFolder, AssetGroupType isOriginal);
	
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
		
		// TODO make sure caller updates AssetService with this new AssetTask
		// TODO AssetService must link this to existing asset files
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
	
	/**
	 * If asset exists, create new preview, create properties from preview, set them on task.
	 * Otherwise, remove any existing properties from task.
	 * @param folderInfo - optional
	 */
	public void recreateAssetProperties(AssetTask<A> task, OriginalAssetFileInfo fileInfo) {
		if (fileInfo == null) {
			removeAssetProperties(task, AssetGroupType.ORIGINAL);
		} else {
			overwriteAssetProperties(task, fileInfo);
			logger.debug("Set properties of original asset file '" + task.getAssetName() + "' on task" + task);
		}
	}
	
	/**
	 * If folder contains asset file, create new preview, create properties, set them on task.
	 * Otherwise, remove any existing properties from task.
	 * @param folderInfo - optional
	 */
	public void recreateAssetProperties(AssetTask<A> task, NewAssetFolderInfo folderInfo) {
		if (folderInfo == null || folderInfo.getStatus() == AssetFolderStatus.VALID_NO_ASSET) {
			removeAssetProperties(task, AssetGroupType.NEW);
		} else if (folderInfo.getStatus() == AssetFolderStatus.VALID_WITH_ASSET) {
			overwriteAssetProperties(task, folderInfo);
			logger.debug("Set properties of new asset file '" + task.getAssetName() + "' on task" + task);
		} else {
			throw new IllegalArgumentException("Invalid asset folder state, cannot recreate preview!");
		}
	}
	
	public boolean isValidAssetProperties(AssetProperties props, AssetInfo info) {
		// TODO check if file length matches props
		// TODO let subclasses check if previewfiles exist
		// TODO check if prop filename matches info filename (exact case-sensitive!)
		return true;
	}
	
	private void overwriteAssetProperties(AssetTask<A> task, AssetInfo info) {
		
		final AssetGroupType type = info.getType();
		
		final Path base = config.assetsBase(type);
		final Path assetPathAbs = base.resolve(info.getAssetFilePathAbsolute(config));
		fileService.restrictAccess(assetPathAbs, base);
		
		final A assetProps = newPropertyInstance(info.getAssetFileName().get(), type);
		assetProps.setFileSize(assetPathAbs.toFile().length());
		
		final String previewFolderName = info.getAssetFileName().getFolded();
		final Path previewFolder = config.assetPreviews().resolve(previewFolderName);
		recreatePreview(assetPathAbs, previewFolder, assetProps);
		
		task.setAssetProperties(assetProps, type);
	}
	
	/**
	 * Remove existing properties from task, delete preview.
	 */
	public void removeAssetProperties(AssetTask<A> task, AssetGroupType isOriginal) {
		if (task.getAssetProperties(isOriginal) == null) {
			return;
		}
		task.setAssetProperties(null, isOriginal);
		final String previewFolderName = task.getAssetName().getFolded();
		final Path previewFolder = config.assetPreviews().resolve(previewFolderName);
		deletePreview(previewFolder, isOriginal);
		final String type = isOriginal.isOriginal() ? "original asset file" : "new asset file";
		logger.debug("Removed properties of " + type + " '" + task.getAssetName() + "' from task " + task);
	}
	
	// TODO caller must check if the linked task has a new asset folder, otherwise it must ask the user to specifify asset folder path
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
	
	// TODO caller must remove asset properties if this was an asset
	public void deleteFile(Path relativeFile, AssetTask<A> task, NewAssetFolderInfo folderInfo, boolean isAsset) {
		//Restrict access
		final Path assetFolder = config.assetsNew().resolve(folderInfo.getAssetFolder());
		final Path visible = assetFolder.resolve(isAsset ? config.subAssets() : config.subOther());
		final Path assetPath = visible.resolve(fileService.restrictAccess(relativeFile, visible));
//		//Delete previews
//		if (isAsset) {
//			removeAssetProperties(task, AssetGroupType.NEW);
//		}
		//Delete file
		fileService.delete(assetPath);
	}
	
	public FileExtensionFilter getExtensionFilter() {
		return extensionFilter;
	}
}
