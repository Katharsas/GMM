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
		Path assetPath = Paths.get(form.getAssetPath());
		//check if path is below valid dirs
		assetPath = fileService.restrictAccess(assetPath, config.ASSETS_ORIGINAL);
		//check conflict with existing files
		Path newAssetPathConflict = config.ASSETS_NEW.resolve(assetPath);
		if(newAssetPathConflict.toFile().exists()) {
			throw new IllegalArgumentException("Asset path \""+assetPath+"\" is invalid. Path points to an existing file or directory!");
		}
		//substitute wildcards
		task.setName(task.getName().replace("%filename%", assetPath.getFileName().toString()));
		task.setDetails(task.getDetails().replace("%filename%", assetPath.getFileName().toString()));

		task.setNewAssetFolder(assetPath);
		task.setOriginalAsset(createAsset(config.ASSETS_ORIGINAL, assetPath));
		createPreview(config.ASSETS_ORIGINAL.resolve(assetPath), task, true);
	}
	
	protected abstract A createAsset(Path base, Path relative) throws IOException;
	protected abstract void createPreview(Path sourceFile, T targetTask, boolean original) throws IOException;
}
