package gmm.service.tasks;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

import gmm.domain.User;
import gmm.domain.task.asset.Asset;
import gmm.domain.task.asset.AssetGroupType;
import gmm.domain.task.asset.AssetTask;
import gmm.service.FileService;
import gmm.service.FileService.FileExtensionFilter;
import gmm.service.data.DataConfigService;
import gmm.web.forms.TaskForm;
import gmm.web.sessions.WorkbenchSession;

/**
 * 
 * @author Jan Mothes
 */
public abstract class AssetTaskService<A extends Asset> extends TaskFormService<AssetTask<A>>{
	
	@Autowired private DataConfigService config;
	@Autowired private FileService fileService;
	@Autowired private WorkbenchSession session;
	
	protected abstract AssetTask<A> createNew(Path assetPath, User user);
	public abstract A createAsset(Path fileName, AssetGroupType isOriginal);
	public abstract void createPreview(Path sourceFile, Path previewFolder, A asset);
	public abstract FileExtensionFilter getExtensions();
	
	@Override
	public final AssetTask<A> create(TaskForm form) {
		final Path relative = getAssetPath(form);
		final AssetTask<A> task = createNew(relative, session.getUser());
		
		//If original asset exists, create previews and asset
		final Path original = config.ASSETS_ORIGINAL.resolve(relative);
		if(original.toFile().isFile()) {
			final A asset = createAsset(relative.getFileName(), AssetGroupType.ORIGINAL);
			final Path newAsset = config.ASSETS_NEW.resolve(relative);
			final Path previewFolder = newAsset.resolve(config.SUB_PREVIEW);
			createPreview(original, previewFolder, asset);
			task.setOriginalAsset(asset);
		}
		edit(task, form);
		return task;
	}
	
	private Path getAssetPath(TaskForm form) {
		final String assetPathString = form.getAssetPath();
		if(assetPathString.equals("")) {
			throw new IllegalStateException("Form does not contain any asset path. Cannot create asset task!");
		}
		return fileService.restrictAccess(Paths.get(assetPathString), config.ASSETS_NEW);
	}
	
	@Override
	public void edit(AssetTask<A> task, TaskForm form) {
		super.edit(task, form);
		final Path assetPath = getAssetPath(form);
		if(!assetPath.equals(task.getAssetPath())) {
			throw new IllegalArgumentException("Asset path cannot be edited!");
		}
		//Substitute wildcards
		task.setName(form.getName().replace("%filename%", assetPath.getFileName().toString()));
		task.setDetails(form.getDetails().replace("%filename%", assetPath.getFileName().toString()));
	}
	
	@Override
	public TaskForm prepareForm(AssetTask<A> task) {
		final TaskForm form = super.prepareForm(task);
		form.setAssetPath(task.getAssetPath().toString());
		return form;
	}
	
	public void addFile(MultipartFile file, AssetTask<A> task) {
		final String fileName = file.getOriginalFilename();
		final boolean isAsset = getExtensions().test(fileName);
		//Add file
		final Path assetFolder = config.ASSETS_NEW.resolve(task.getAssetPath());
		final Path filePath = assetFolder
				.resolve(isAsset ? config.SUB_ASSETS : config.SUB_OTHER)
				.resolve(fileName);
		try {
			fileService.createFile(filePath, file.getBytes());
		} catch (final IOException e) {
			throw new UncheckedIOException("Could not retrieve bytes from uploaded file '"+fileName+"'!", e);
		}
		
		if(isAsset) {
			final A asset = createAsset(Paths.get(fileName), AssetGroupType.NEW);
			final Path previewFolder = assetFolder.resolve(config.SUB_PREVIEW);
			createPreview(filePath, previewFolder, asset);
			task.setNewestAsset(asset);
		}
	}
	
	public abstract void deletePreview(Path taskFolder);
	
	public void deleteFile(AssetTask<A> task, Path relativeFile, boolean isAsset) {
		//Restrict access
		final Path taskFolder = config.ASSETS_NEW.resolve(task.getAssetPath());
		final Path visible = taskFolder.resolve(isAsset ? config.SUB_ASSETS : config.SUB_OTHER);
		final Path assetPath = visible.resolve(fileService.restrictAccess(relativeFile, visible));
		//Delete previews
		final A newestAsset = task.getNewestAsset();
		if(isAsset && newestAsset != null && 
				assetPath.getFileName().toString().equals(newestAsset.getFileName())) {
			deletePreview(taskFolder);
			task.setNewestAsset(null);
		}
		//Delete file
		fileService.delete(assetPath);
	}
}
