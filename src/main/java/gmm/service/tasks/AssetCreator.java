package gmm.service.tasks;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;

import gmm.domain.Asset;
import gmm.domain.AssetTask;
import gmm.service.FileService;
import gmm.service.data.DataConfigService;
import gmm.web.forms.TaskForm;

public abstract class AssetCreator<A extends Asset, T extends AssetTask<A>> {
	
	@Autowired DataConfigService config;
	@Autowired FileService fileService;
	
	public void editAssetTask(T task, TaskForm form) throws IOException {
		String newFolderString = form.getNewAssetFolderPath();
		String originalString = form.getOriginalAssetPath();
		
		if(newFolderString.equals("")) {
			if(originalString.equals("")) {
				throw new IllegalStateException("Form does not contain any asset path. Cannot create asset task!");
			}
			else {
				newFolderString = originalString;
			}
		}
		//Set newFolderPath
		Path newFolder = Paths.get(newFolderString);
		newFolder = fileService.restrictAccess(newFolder, config.ASSETS_NEW);
		Path conflict = config.ASSETS_NEW.resolve(newFolder);
		if(conflict.toFile().exists()) {
			throw new IllegalArgumentException("New asset path \""+newFolder+"\" is invalid. Path points to an existing file or directory!");
		}
		//substitute wildcards
		task.setName(task.getName().replace("%filename%", newFolder.getFileName().toString()));
		task.setDetails(task.getDetails().replace("%filename%", newFolder.getFileName().toString()));
		task.setNewAssetFolder(newFolder);

		//If not empty, set originalAsset
		if(!originalString.equals("")) {
			Path originalPath = Paths.get(originalString);
			originalPath = fileService.restrictAccess(originalPath, config.ASSETS_ORIGINAL);
			task.setOriginalAsset(createAsset(config.ASSETS_ORIGINAL, originalPath));
			createPreview(config.ASSETS_ORIGINAL.resolve(originalPath), task, true);
		}
	}
	
	protected abstract A createAsset(Path base, Path relative) throws IOException;
	protected abstract void createPreview(Path sourceFile, T targetTask, boolean original) throws IOException;
}
