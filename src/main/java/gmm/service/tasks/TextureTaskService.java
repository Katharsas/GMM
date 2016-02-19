package gmm.service.tasks;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;

import org.imgscalr.Scalr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gmm.domain.User;
import gmm.domain.task.asset.AssetGroupType;
import gmm.domain.task.asset.Texture;
import gmm.domain.task.asset.TextureTask;
import gmm.service.FileService;
import gmm.service.FileService.FileExtensionFilter;
import gmm.service.data.DataConfigService;

/**
 * Should maybe be joined with {@link TextureAssetService} after Mesh implementation.
 * 
 * @author Jan
 */
@Service
public class TextureTaskService extends AssetTaskService<Texture> {

	@Autowired private DataConfigService config;
	@Autowired private FileService fileService;
	
	public static final FileExtensionFilter extensions =
			new FileService.FileExtensionFilter(new String[] {"tga"});
	
	private static final int SMALL_SIZE = 300;
	
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
	public void createPreview(Path sourceFile, Path preview, Texture asset) {
		BufferedImage image = readImage(sourceFile);
		asset.setDimensions(image.getHeight(), image.getWidth());
		Path targetFile;
		final String version = asset.getGroupType().getPreviewFileName();
		//full preview 
		targetFile = preview.resolve(version+"_full.png");
		writeImage(image, targetFile);
		//small preview
		targetFile = preview.resolve(version+"_small.png");
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
	public void deletePreview(Path taskFolder) {
		final Path preview = taskFolder.resolve(config.SUB_PREVIEW);
		final String version = AssetGroupType.NEW.getPreviewFileName();
		Path previewFile;
		previewFile = preview.resolve(version + "_full.png");
		if(previewFile.toFile().exists()) {
			fileService.delete(previewFile);
		}
		previewFile = preview.resolve(version + "_small.png");
		if(previewFile.toFile().exists()) {
			fileService.delete(previewFile);
		}
	}

	@Override
	public Texture createAsset(Path relative, AssetGroupType isOriginal) {
		return new Texture(relative, isOriginal);
	}

	@Override
	public Class<TextureTask> getTaskType() {
		return TextureTask.class;
	}

	@Override
	protected TextureTask createNew(Path assetPath, User user) {
		return new TextureTask(user, assetPath);
	}
	
	@Override
	public FileExtensionFilter getExtensions() {
		return TextureTaskService.extensions;
	}
	
	/**
	 * Returns a texture preview image (png) as byte array.
	 */
	public byte[] getPreview(TextureTask task, boolean small, String version) throws IOException {
		final String imageName = version + "_" + (small ? "small" : "full") + ".png";		
		
		final Path imagePath = config.ASSETS_NEW
				.resolve(task.getAssetPath())
				.resolve(config.SUB_PREVIEW)
				.resolve(imageName);

		if(!imagePath.toFile().exists()) {
			return null;
		}
		final BufferedImage image = ImageIO.read(imagePath.toFile());
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(image, "png", out);
		final byte[] bytes = out.toByteArray();
		return bytes;
	}
}
