package gmm.service.tasks;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gmm.domain.User;
import gmm.domain.task.asset.AssetGroupType;
import gmm.domain.task.asset.Model;
import gmm.domain.task.asset.ModelTask;
import gmm.service.FileService;
import gmm.service.FileService.FileExtensionFilter;
import gmm.service.data.DataConfigService;
import gmm.service.tasks.PythonTCPSocket.MeshData;

@Service
public class ModelTaskService extends AssetTaskService<Model> {

	@Autowired private DataConfigService config;
	@Autowired private FileService fileService;
	@Autowired private PythonTCPSocket python;
	
	public static final FileExtensionFilter extensions =
			new FileService.FileExtensionFilter(new String[] {"3ds"});
	
	@Override
	public Model createAsset(Path fileName, AssetGroupType isOriginal) {
		return new Model(fileName, isOriginal);
	}

	@Override
	public void createPreview(Path sourceFile, Path previewPath, Model asset) {
		fileService.createDirectory(previewPath);
		final String version = asset.getGroupType().getPreviewFileName();
		final Path target = previewPath.resolve(version + ".js");
		MeshData meshData = python.createPreview(sourceFile, target);
		asset.setPolyCount(meshData.getPolygonCount());
	}
	
	@Override
	public void deletePreview(Path taskFolder) {
		final Path previews = taskFolder.resolve(config.SUB_PREVIEW);
		final String version = AssetGroupType.NEW.getPreviewFileName();
		final Path previewFile = previews.resolve(version + ".js");
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
