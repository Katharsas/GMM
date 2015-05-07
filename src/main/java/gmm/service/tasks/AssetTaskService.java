package gmm.service.tasks;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

import gmm.domain.User;
import gmm.domain.task.Asset;
import gmm.domain.task.AssetTask;
import gmm.service.FileService;
import gmm.service.FileService.FileExtensionFilter;
import gmm.service.data.DataConfigService;
import gmm.web.forms.TaskForm;
import gmm.web.sessions.TaskSession;

/**
 * 
 * @author Jan Mothes
 */
public abstract class AssetTaskService<A extends Asset, T extends AssetTask<A>> extends TaskFormService<T>{
	
	@Autowired private DataConfigService config;
	@Autowired private FileService fileService;
	@Autowired private TaskSession session;
	
	protected abstract T createNew(Path assetPath, User user) throws Exception;
	
	@Override
	public final T create(TaskForm form) throws Exception {
		final Path assetPath = getAssetPath(form);
		T task = createNew(assetPath, session.getUser());
		
		//If original asset exists, create previews and asset
		Path originalAbsolute = config.ASSETS_ORIGINAL.resolve(assetPath);
		if(originalAbsolute.toFile().isFile()) {
			task.setOriginalAsset(createAsset(assetPath.getFileName()));
			createPreview(originalAbsolute, task, true);
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
	public void edit(T task, TaskForm form) throws IOException {
		super.edit(task, form);
		final Path assetPath = getAssetPath(form);
		//Substitute wildcards
		task.setName(form.getName().replace("%filename%", assetPath.getFileName().toString()));
		task.setDetails(form.getDetails().replace("%filename%", assetPath.getFileName().toString()));
	}
	
	@Override
	public TaskForm prepareForm(T task) {
		TaskForm form = super.prepareForm(task);
		form.setAssetPath(task.getAssetPath().toString());
		return form;
	}
	
	public abstract A createAsset(Path fileName);
	public abstract void createPreview(Path sourceFile, T task, boolean original) throws IOException;
	public abstract FileExtensionFilter getExtensions();
	
	public void addFile(MultipartFile file, T task) throws IOException {
		String fileName = file.getOriginalFilename();
		boolean isAsset = getExtensions().accept(null, fileName);
		//Add file
		Path relative = task.getAssetPath()
				.resolve(isAsset ? config.SUB_ASSETS : config.SUB_OTHER)
				.resolve(fileName);
		Path assetPath = config.ASSETS_NEW.resolve(relative);
		fileService.createFile(assetPath, file.getBytes());
		
		if(isAsset) {
			task.setNewestAsset(createAsset(Paths.get(fileName)));
			createPreview(assetPath, task, false);
		}
	}
	
	public abstract void deletePreview(Path taskFolder) throws IOException;
	
	public void deleteFile(T task, Path relativeFile, boolean isAsset) throws IOException {
		//Restrict access
		Path taskFolder = config.ASSETS_NEW.resolve(task.getAssetPath());
		Path visible = taskFolder.resolve(isAsset ? config.SUB_ASSETS : config.SUB_OTHER);
		Path assetPath = visible.resolve(fileService.restrictAccess(relativeFile, visible));
		//Delete previews
		A newestAsset = task.getNewestAsset();
		if(isAsset && newestAsset != null && 
				assetPath.getFileName().toString().equals(newestAsset.getFileName())) {
			deletePreview(taskFolder);
			task.setNewestAsset(null);
		}
		//Delete file
		fileService.delete(assetPath);
	}
}
