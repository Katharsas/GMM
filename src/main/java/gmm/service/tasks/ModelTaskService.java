package gmm.service.tasks;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gmm.collections.HashSet;
import gmm.collections.Set;
import gmm.domain.User;
import gmm.domain.task.TaskType;
import gmm.domain.task.asset.AssetGroupType;
import gmm.domain.task.asset.AssetName;
import gmm.domain.task.asset.ModelProperties;
import gmm.domain.task.asset.ModelTask;
import gmm.service.FileService;
import gmm.service.data.Config;
import gmm.service.data.DataAccess;
import gmm.service.tasks.PythonTCPSocket.MeshData;

@Service
public class ModelTaskService extends AssetTaskService<ModelProperties>
		implements ServletContextListener {

	private final PythonTCPSocket python;
	
	private static final String[] extensions = new String[] {"3ds"};
	
	@Autowired
	public ModelTaskService(DataAccess data, Config config, FileService fileService,
			PythonTCPSocket python) {
		super(data, config, fileService);
		this.python = python;
	}

	@Override
	protected String[] getExtensions() {
		return extensions;
	}

	@Override
	public CompletableFuture<ModelProperties> recreatePreview(
			Path sourceFile, Path previewFolder, AssetGroupType type) {
		
		return CompletableFuture.supplyAsync(() -> {
			
			fileService.createDirectory(previewFolder);
			final Path target = getPreviewFilePath(previewFolder, type);
			fileService.testReadFile(sourceFile);
			
			deletePreview(target);
			final MeshData meshData = python.createPreview(sourceFile, target);
			final Set<String> textures = new HashSet<>(String.class, meshData.getTextures());
			final Set<AssetName> textureNames = new HashSet<>(AssetName.class);
			for(final String name : textures) {
				textureNames.add(new AssetName(name));
			}
			final ModelProperties asset = new ModelProperties(
					meshData.getPolygonCount(), textureNames);
			return asset;
		}, threadPool);
	}
	
	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		super.shutdown();
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
	
	/**
	 * Deletes preview if it exist.
	 */
	private void deletePreview(Path previewFile) {
		if(previewFile.toFile().exists()) {
			fileService.delete(previewFile);
		}
	}

	@Override
	public TaskType getTaskType() {
		return TaskType.MESH;
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
				.resolve(task.getAssetName().getKey().toString())
				.resolve(modelName);
		try(FileInputStream fis = new FileInputStream(path.toFile())) {
			IOUtils.copy(fis, target);
		} catch (final IOException e) {
			throw new UncheckedIOException(
					"Could not write preview file from '" + path.toString() + "' to stream!", e);
		}
	}
}
