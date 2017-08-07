package gmm.service.tasks;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;

import org.apache.commons.io.IOUtils;
import org.imgscalr.Scalr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gmm.domain.User;
import gmm.domain.task.asset.AssetGroupType;
import gmm.domain.task.asset.AssetName;
import gmm.domain.task.asset.TextureProperties;
import gmm.domain.task.asset.TextureTask;
import gmm.service.FileService;
import gmm.service.data.DataConfigService;

/**
 * Should maybe be joined with {@link TextureAssetService} after Mesh implementation.
 * 
 * @author Jan
 */
@Service
public class TextureTaskService extends AssetTaskService<TextureProperties> {

	@Autowired private DataConfigService config;
	@Autowired private FileService fileService;
	
	private static final String[] extensions = new String[] {"tga"};
	
	private static final int SMALL_SIZE = 300;
	
	@Override
	protected String[] getExtensions() {
		return extensions;
	}
	
	@PostConstruct
	private void init() {
		//register TGA loader plugin
		final IIORegistry registry = IIORegistry.getDefaultInstance();
		registry.registerServiceProvider(new com.realityinteractive.imageio.tga.TGAImageReaderSpi());
	}
	
	/**
	 * Create Preview files from texture file.
	 * For more texture operations see {@link gmm.service.tasks.TextureAssetService}
	 */
	@Override
	public void recreatePreview(Path sourceFile, Path previewFolder, TextureProperties asset) {
		BufferedImage image = readImage(sourceFile);
		asset.setDimensions(image.getHeight(), image.getWidth());
		Path targetFile;
		//full preview 
		targetFile = getPreviewFilePath(previewFolder, asset.getGroupType(), false);
		writeImage(image, targetFile);
		//small preview
		targetFile = getPreviewFilePath(previewFolder, asset.getGroupType(), true);
		if(image.getHeight() > SMALL_SIZE || image.getWidth() > SMALL_SIZE){
			image = Scalr.resize(image, SMALL_SIZE);
		}
		writeImage(image, targetFile);
	}
	
	private BufferedImage readImage(Path file) {
		try {
			return ImageIO.read(file.toFile());
		} catch (final IOException e) {
			throw new UncheckedIOException("Could not read image at '" + file + "'!", e);
		}
	}
	
	private void writeImage(BufferedImage image, Path targetFile) {
		fileService.createDirectory(targetFile.getParent());
		try {
			ImageIO.write(image, "png", targetFile.toFile());
		} catch (final IOException e) {
			throw new UncheckedIOException("Could not write image to '" + targetFile + ".png'!", e);
		}
	}
	
	@Override
	public void deletePreview(Path previewFolder, AssetGroupType isOriginal) {
		if (previewFolder.toFile().exists()) {
			final Path targetFull = getPreviewFilePath(previewFolder, isOriginal, false);
			final Path targetSmall = getPreviewFilePath(previewFolder, isOriginal, true);
			if (targetFull.toFile().exists()) fileService.delete(targetSmall);
			if (targetSmall.toFile().exists()) fileService.delete(targetSmall);
		}
	}
	
	@Override
	protected boolean hasPreview(Path previewFolder, AssetGroupType isOriginal) {
		final Path targetFull = getPreviewFilePath(previewFolder, isOriginal, false);
		final Path targetSmall = getPreviewFilePath(previewFolder, isOriginal, true);
		return targetFull.toFile().isFile() && targetSmall.toFile().isFile();
	}
	
	private Path getPreviewFilePath(Path previewFolder, AssetGroupType isOriginal, boolean isSmall) {
		final String isOriginalString = isOriginal.getPreviewFileName();
		return previewFolder.resolve(isOriginalString + (isSmall ? "_small.png" : "_full.png"));
	}

	@Override
	protected TextureProperties newPropertyInstance(String filename, AssetGroupType isOriginal) {
		return new TextureProperties(filename, isOriginal);
	}

	@Override
	public Class<TextureTask> getTaskType() {
		return TextureTask.class;
	}

	@Override
	protected TextureTask newInstance(AssetName assetName, User user) {
		return new TextureTask(user, assetName);
	}
	
	@Override
	public Path getAssetTypeSubFolder() {
		return config.subNewTextures();
	}
	
	/**
	 * Returns a texture preview image (png) as byte array.
	 */
	public void writePreview(TextureTask task, boolean small, String version, OutputStream target) {
		final String imageName = version + (small ? "_small" : "_full") + ".png";		
		final Path path = config.assetPreviews()
				.resolve(task.getAssetName().getFolded())
				.resolve(imageName);
		try(FileInputStream fis = new FileInputStream(path.toFile())) {
			IOUtils.copy(fis, target);
		} catch (final IOException e) {
			throw new UncheckedIOException(
					"Could not deliver preview file from '" + path.toString() + "'!", e);
		}
	}
}
