package gmm.service.tasks;

import java.nio.file.Path;

import org.springframework.stereotype.Service;

import gmm.domain.User;
import gmm.domain.task.AssetTask;
import gmm.domain.task.Model;
import gmm.domain.task.ModelTask;
import gmm.service.FileService;
import gmm.service.FileService.FileExtensionFilter;

@Service
public class ModelTaskService extends AssetTaskService<Model> {

	public static final FileExtensionFilter extensions =
			new FileService.FileExtensionFilter(new String[] {"3ds"});
	
	@Override
	public Model createAsset(Path fileName) {
		return new Model(fileName);
	}

	@Override
	public void createPreview(Path sourceFile, AssetTask<Model> task, boolean original) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void deletePreview(Path taskFolder) {
		// TODO Auto-generated method stub
		
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
