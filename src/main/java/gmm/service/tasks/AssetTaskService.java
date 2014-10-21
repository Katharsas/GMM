package gmm.service.tasks;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;

import gmm.domain.Asset;
import gmm.domain.AssetTask;
import gmm.domain.User;
import gmm.service.FileService;
import gmm.service.data.DataConfigService;
import gmm.web.forms.TaskForm;
import gmm.web.sessions.TaskSession;

/**
 * 
 * @author Jan Mothes
 */
public abstract class AssetTaskService<A extends Asset, T extends AssetTask<A>> extends TaskService<T>{
	
	@Autowired private DataConfigService config;
	@Autowired private FileService fileService;
	@Autowired private TaskSession session;
	
	protected abstract T createNew(Path assetPath, User user);
	
	@Override
	public final T create(TaskForm form) throws IOException {
		//get asset path
		String assetPathString = form.getAssetPath();
		Path assetPath = Paths.get(assetPathString);
		if(assetPathString.equals("")) {
			throw new IllegalStateException("Form does not contain any asset path. Cannot create asset task!");
		}
		assetPath = fileService.restrictAccess(assetPath, config.ASSETS_NEW);
		//create asset task
		T task = createNew(assetPath, session.getUser());
		
		//Substitute wildcards
		form.setName(form.getName().replace("%filename%", assetPath.getFileName().toString()));
		form.setDetails(form.getDetails().replace("%filename%", assetPath.getFileName().toString()));

		//If original asset exists, create previews and asset
		Path originalAbsolute = config.ASSETS_ORIGINAL.resolve(assetPath);
		if(originalAbsolute.toFile().isFile()) {
			createPreview(originalAbsolute, task, true);
			task.setOriginalAsset(createAsset(null, task));
		}
		super.edit(task, form);
		return task;
	}
	
	@Override
	public TaskForm prepareForm(T task) {
		TaskForm form = super.prepareForm(task);
		form.setAssetPath(task.getAssetPath().toString());
		return form;
	}
	
	public abstract A createAsset(Path relative, T owner) throws IOException;
	public abstract void createPreview(Path sourceFile, T targetTask, boolean original) throws IOException;
}
