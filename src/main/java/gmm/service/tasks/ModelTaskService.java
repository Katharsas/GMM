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
import gmm.domain.task.asset.AssetName;
import gmm.domain.task.asset.ModelProperties;
import gmm.domain.task.asset.ModelTask;
import gmm.service.FileService;
import gmm.service.data.DataConfigService;
import gmm.service.tasks.PythonTCPSocket.MeshData;

@Service
public class ModelTaskService extends AssetTaskService<ModelProperties> {

	@Autowired private DataConfigService config;
	@Autowired private FileService fileService;
	@Autowired private PythonTCPSocket python;
	
	private static final String[] extensions = new String[] {"3ds"};
	
	@Override
	protected String[] getExtensions() {
		return extensions;
	}
	
	@Override
	protected ModelProperties newPropertyInstance(AssetGroupType isOriginal) {
		return new ModelProperties(isOriginal);
	}

	@Override
	public void recreatePreview(Path sourceFile, Path previewFolder, ModelProperties asset) {
		fileService.createDirectory(previewFolder);
		final String isOriginalString = asset.getGroupType().getPreviewFileName();
		final Path target = previewFolder.resolve(isOriginalString + ".json");
		deletePreview(target);
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
	public void deletePreview(Path previewFolder, AssetGroupType isOriginal) {
		final String isOriginalString = isOriginal.getPreviewFileName();
		final Path target = previewFolder.resolve(isOriginalString + ".json");
		deletePreview(target);
	}
	
	
	private void deletePreview(Path previewFile) {
		if(previewFile.toFile().exists()) {
			fileService.delete(previewFile);
		}
	}

	@Override
	public Class<ModelTask> getTaskType() {
		return ModelTask.class;
	}

	@Override
	protected ModelTask newInstance(AssetName assetName, User user) {
		return new ModelTask(user, assetName);
	}
	
	@Override
	public Path getAssetTypeSubFolder() {
		return config.subNewModels();
	}
	
	public void writePreview(ModelTask task, String version, OutputStream target) {
		final String modelName = version + ".json";		
		final Path path = config.assetPreviews()
				.resolve(task.getAssetName().getFolded())
				.resolve(modelName);
		try(FileInputStream fis = new FileInputStream(path.toFile())) {
			IOUtils.copy(fis, target);
		} catch (final IOException e) {
			throw new UncheckedIOException(
					"Could not deliver preview file from '" + path.toString() + "'!", e);
		}
	}
}
