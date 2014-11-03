package gmm.service.tasks;

import gmm.domain.Model;
import gmm.domain.ModelTask;
import gmm.domain.User;
import gmm.service.FileService;
import gmm.service.FileService.FileExtensionFilter;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.stereotype.Service;

@Service
public class ModelTaskService extends AssetTaskService<Model, ModelTask> {

	public static final FileExtensionFilter extensions =
			new FileService.FileExtensionFilter(new String[] {"3ds"});
	
	@Override
	public Model createAsset(Path fileName) {
		return new Model(fileName);
	}

	@Override
	public void createPreview(Path sourceFile, ModelTask task,
			boolean original) throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void deletePreview(Path taskFolder) throws IOException {
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
	
	@Override
	public FileExtensionFilter getExtensions() {
		return ModelTaskService.extensions;
	}
}
