package gmm.service.tasks;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gmm.collections.HashSet;
import gmm.collections.Set;
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
		final Path target = previewPath.resolve(version + ".json");
		final MeshData meshData = python.createPreview(sourceFile, target);
		final Set<String> texturePaths = new HashSet<>(String.class, meshData.getTextures());
		final Set<String> textureNames = new HashSet<>(String.class);
		for(final String path : texturePaths) {
			textureNames.add(Paths.get(path).getFileName().toString());
		}
		asset.setTextureNames(textureNames);
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
	
	public void writePreview(ModelTask task, String version, OutputStream target) {
		final String modelName = version + ".json";		
		final Path path = config.ASSETS_NEW
				.resolve(task.getAssetPath())
				.resolve(config.SUB_PREVIEW)
				.resolve(modelName);
		try(FileInputStream fis = new FileInputStream(path.toFile())) {
			IOUtils.copy(fis, target);
		} catch (final IOException e) {
			throw new UncheckedIOException(
					"Could not deliver preview file from '" + path.toString() + "'!", e);
		}
	}
}
