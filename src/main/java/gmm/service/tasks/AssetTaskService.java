package gmm.service.tasks;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

import gmm.domain.User;
import gmm.domain.task.asset.AssetGroupType;
import gmm.domain.task.asset.AssetName;
import gmm.domain.task.asset.AssetProperties;
import gmm.domain.task.asset.AssetTask;
import gmm.service.FileService;
import gmm.service.FileService.FileExtensionFilter;
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
	
	@Autowired private DataConfigService config;
	@Autowired private FileService fileService;
	
	protected abstract A newPropertyInstance(AssetGroupType isOriginal);
	
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
		final String assetName = getAssetName(form);
		if(!assetName.equals(task.getAssetName())) {
			throw new IllegalArgumentException("Asset path cannot be edited!");
			// TODO if caller makes AssetNameConflictCheck and uses AssetService to relink assets, asset name editing could be enabled.
		}
		//Substitute wildcards
		task.setName(form.getName().replace("%filename%", assetName));
		task.setDetails(form.getDetails().replace("%filename%", assetName));
	}
	
	@Override
	public TaskForm prepareForm(AssetTask<A> task) {
		final TaskForm form = super.prepareForm(task);
		form.setAssetName(task.getAssetName().toString());
		return form;
	}
	
	/**
	 * Create new preview, create properties from preview, set them on task.
	 */
	public void recreateAssetProperties(AssetTask<A> task, OriginalAssetFileInfo fileInfo) {
		final Path assetPathAbs = config.assetsOriginal().resolve(fileInfo.getAssetFile());
		fileService.restrictAccess(assetPathAbs, config.assetsOriginal());
		
		final A assetProps = newPropertyInstance(AssetGroupType.ORIGINAL);
		assetProps.setFileSize(assetPathAbs.toFile().length());
		
		final String previewFolderName = fileInfo.getAssetFileName().getFolded();
		final Path previewFolder = config.assetPreviews().resolve(previewFolderName);
		recreatePreview(assetPathAbs, previewFolder, assetProps);
		
		task.setOriginalAsset(assetProps);
	}
	
	/**
	 * If folder contains asset file, create new preview, create properties, set them on task.
	 * Otherwise, remove any existing properties from task.
	 */
	public void recreateAssetProperties(AssetTask<A> task, NewAssetFolderInfo folderInfo) {
		if (folderInfo.getStatus() == AssetFolderStatus.VALID_NO_ASSET) {
			task.setNewAsset(null);
		} else if (folderInfo.getStatus() == AssetFolderStatus.VALID_WITH_ASSET) {
			
			final Path assetPathAbs = config.assetsNew().resolve(folderInfo.getAssetFolder()
					.resolve(config.subAssets().resolve(folderInfo.getAssetFileName().get())));
			fileService.restrictAccess(assetPathAbs, config.assetsNew());
			
			final A assetProps = newPropertyInstance(AssetGroupType.NEW);
			assetProps.setFileSize(assetPathAbs.toFile().length());
			
			final String previewFolderName = folderInfo.getAssetFileName().getFolded();
			final Path previewFolder = config.assetPreviews().resolve(previewFolderName);
			recreatePreview(assetPathAbs, previewFolder, assetProps);
			
			task.setNewAsset(assetProps);
		} else {
			throw new IllegalArgumentException("Invalid asset folder state, cannot recreate preview!");
		}
	}
	
	/**
	 * Remove existing properties from task, delete preview.
	 */
	public void removeAssetProperties(AssetTask<A> task, AssetGroupType isOriginal) {
		if(isOriginal.isOriginal()) {
			task.setOriginalAsset(null);
		} else {
			task.setNewAsset(null);
		}
		final String previewFolderName = task.getAssetName().getFolded();
		final Path previewFolder = config.assetPreviews().resolve(previewFolderName);
		deletePreview(previewFolder, isOriginal);
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
