package gmm.service.tasks;

import java.io.IOException;
import java.io.UncheckedIOException;
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
public abstract class AssetTaskService<A extends Asset> extends TaskFormService<AssetTask<A>>{
	
	@Autowired private DataConfigService config;
	@Autowired private FileService fileService;
	@Autowired private TaskSession session;
	
	protected abstract AssetTask<A> createNew(Path assetPath, User user);
	
	@Override
	public final AssetTask<A> create(TaskForm form) {
		final Path assetPath = getAssetPath(form);
		final AssetTask<A> task = createNew(assetPath, session.getUser());
		
		//If original asset exists, create previews and asset
		final Path originalAbsolute = config.ASSETS_ORIGINAL.resolve(assetPath);
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
	public void edit(AssetTask<A> task, TaskForm form) {
		super.edit(task, form);
		final Path assetPath = getAssetPath(form);
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
	
	public abstract A createAsset(Path fileName);
	public abstract void createPreview(Path sourceFile, AssetTask<A> task, boolean original);
	public abstract FileExtensionFilter getExtensions();
	
	public void addFile(MultipartFile file, AssetTask<A> task) {
		final String fileName = file.getOriginalFilename();
		final boolean isAsset = getExtensions().test(fileName);
		//Add file
		final Path relative = task.getAssetPath()
				.resolve(isAsset ? config.SUB_ASSETS : config.SUB_OTHER)
				.resolve(fileName);
		final Path assetPath = config.ASSETS_NEW.resolve(relative);
		try {
			fileService.createFile(assetPath, file.getBytes());
		} catch (final IOException e) {
			throw new UncheckedIOException("Could not retrieve bytes from uploaded file '"+fileName+"'!", e);
		}
		
		if(isAsset) {
			task.setNewestAsset(createAsset(Paths.get(fileName)));
			createPreview(assetPath, task, false);
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
