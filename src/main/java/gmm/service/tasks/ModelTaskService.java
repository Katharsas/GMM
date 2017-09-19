package gmm.service.tasks;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

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
	protected ModelProperties newPropertyInstance() {
		return new ModelProperties();
	}

	@Override
	public CompletableFuture<ModelProperties> recreatePreview(
			Path sourceFile, Path previewFolder, AssetGroupType type, ModelProperties asset) {
		
		fileService.createDirectory(previewFolder);
		final Path target = getPreviewFilePath(previewFolder, type);
		deletePreview(target);
		final MeshData meshData = python.createPreview(sourceFile, target);
		final Set<String> texturePaths = new HashSet<>(String.class, meshData.getTextures());
		final Set<String> textureNames = new HashSet<>(String.class);
		for(final String path : texturePaths) {
			textureNames.add(Paths.get(path).getFileName().toString());
		}
		asset.setTextureNames(textureNames);
		asset.setPolyCount(meshData.getPolygonCount());
		return CompletableFuture.completedFuture(asset);
	}
	
	@Override
	public void deletePreview(Path previewFolder, AssetGroupType isOriginal) {
		final Path target = getPreviewFilePath(previewFolder, isOriginal);
		deletePreview(target);
	}
	
	@Override
	protected boolean hasPreview(Path previewFolder, AssetGroupType isOriginal) {
		final Path target = getPreviewFilePath(previewFolder, isOriginal);
		return target.toFile().isFile();
	}
	
	private Path getPreviewFilePath(Path previewFolder, AssetGroupType isOriginal) {
		final String isOriginalString = isOriginal.getPreviewFileName();
		return previewFolder.resolve(isOriginalString + ".json");
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
				.resolve(task.getAssetName().getKey())
				.resolve(modelName);
		try(FileInputStream fis = new FileInputStream(path.toFile())) {
			IOUtils.copy(fis, target);
		} catch (final IOException e) {
			throw new UncheckedIOException(
					"Could not write preview file from '" + path.toString() + "' to stream!", e);
		}
	}
}
