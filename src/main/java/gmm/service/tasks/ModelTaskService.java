package gmm.service.tasks;

import gmm.domain.Model;
import gmm.domain.ModelTask;
import gmm.domain.User;
import gmm.service.FileService;

import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Path;

import org.springframework.stereotype.Service;

@Service
public class ModelTaskService extends AssetTaskService<Model, ModelTask> {

	public static final FilenameFilter extensions =
			new FileService.FileExtensionFilter(new String[] {"3ds"});
	
	@Override
	public Model createAsset(Path fileName) {
		return new Model(fileName);
	}

	@Override
	public void createPreview(Path sourceFile, ModelTask targetTask,
			boolean original) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Class<ModelTask> getTaskType() {
		return ModelTask.class;
	}

	@Override
	protected ModelTask createNew(Path assetPath, User user) throws Exception {
		return new ModelTask(user, assetPath);
	}
}
