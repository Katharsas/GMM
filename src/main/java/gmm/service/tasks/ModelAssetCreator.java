package gmm.service.tasks;

import gmm.domain.Model;
import gmm.domain.ModelTask;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.stereotype.Service;

@Service
public class ModelAssetCreator extends AssetCreator<Model, ModelTask> {

	@Override
	protected Model createAsset(Path base, Path relative) {
		return new Model(base, relative);
	}

	@Override
	protected void createPreview(Path sourceFile, ModelTask targetTask,
			boolean original) throws IOException {
		// TODO Auto-generated method stub
		
	}
}
