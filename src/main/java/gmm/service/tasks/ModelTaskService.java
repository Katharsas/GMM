package gmm.service.tasks;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gmm.domain.User;
import gmm.domain.task.AssetTask;
import gmm.domain.task.Model;
import gmm.domain.task.ModelTask;
import gmm.service.FileService;
import gmm.service.FileService.FileExtensionFilter;
import gmm.service.data.DataConfigService;

@Service
public class ModelTaskService extends AssetTaskService<Model> {

	@Autowired private DataConfigService config;
	@Autowired private FileService fileService;
	@Autowired private PythonTCPSocket python;
	
	public static final FileExtensionFilter extensions =
			new FileService.FileExtensionFilter(new String[] {"3ds"});
	
	@Override
	public Model createAsset(Path fileName) {
		return new Model(fileName);
	}

	@Override
	public void createPreview(Path sourceFile, AssetTask<Model> task, boolean isOriginal) {
		final Path taskFolder = config.ASSETS_NEW.resolve(task.getAssetPath());
		final String version = isOriginal ? "original" : "newest";
		final Path target = taskFolder.resolve(config.SUB_PREVIEW).resolve(version + ".js");
		python.createPreview(sourceFile, target);
	}
	
	@Override
	public void deletePreview(Path taskFolder) {
		final Path previews = taskFolder.resolve(config.SUB_PREVIEW);
		final Path previewFile = previews.resolve("newest.js");
		if(previewFile.toFile().exists()) {
			fileService.delete(previewFile);
		}
	}

	@Override
	public Class<ModelTask> getTaskType() {
		return ModelTask.class;
	}

	@Override
	protected ModelTask createNew(Path assetPath, User user) {
		return new ModelTask(user, assetPath);
	}
	
	@Override
	public FileExtensionFilter getExtensions() {
		return ModelTaskService.extensions;
	}
}
