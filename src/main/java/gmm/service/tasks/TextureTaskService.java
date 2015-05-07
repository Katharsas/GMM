package gmm.service.tasks;

import gmm.domain.User;
import gmm.domain.task.Texture;
import gmm.domain.task.TextureTask;
import gmm.service.FileService;
import gmm.service.FileService.FileExtensionFilter;
import gmm.service.data.DataConfigService;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;

import org.imgscalr.Scalr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Should maybe be joined with {@link TextureAssetService} after Mesh implementation.
 * 
 * @author Jan
 */
@Service
public class TextureTaskService extends AssetTaskService<Texture, TextureTask> {

	@Autowired private DataConfigService config;
	@Autowired private FileService fileService;
	
	public static final FileExtensionFilter extensions =
			new FileService.FileExtensionFilter(new String[] {"tga"});
	
	private static final int SMALL_SIZE = 300;
	
	@PostConstruct
	private void init() {
		//register TGA loader plugin
		IIORegistry registry = IIORegistry.getDefaultInstance();
		registry.registerServiceProvider(new com.realityinteractive.imageio.tga.TGAImageReaderSpi());
	}
	
	/**
	 * Create Preview files from texture file.
	 * For more texture operations see {@link gmm.service.tasks.TextureAssetService}
	 */
	@Override
	public void createPreview(Path sourceFile, TextureTask task, boolean isOriginal) throws IOException {
		if(!sourceFile.toFile().exists()) {
			return;
		}
		Path taskFolder = config.ASSETS_NEW.resolve(task.getAssetPath());
		Path targetFile;
		String version = isOriginal ? "original" : "newest";
		BufferedImage image = ImageIO.read(sourceFile.toFile());
		Texture asset = isOriginal ? task.getOriginalAsset() : task.getNewestAsset();
		asset.setDimensions(image.getHeight(), image.getWidth());
		
		//full preview 
		targetFile = taskFolder.resolve(config.SUB_PREVIEW).resolve(version+"_full.png");
		fileService.prepareFileCreation(targetFile);
		ImageIO.write(image, "png", targetFile.toFile());
		
		//small preview
		targetFile = taskFolder.resolve(config.SUB_PREVIEW).resolve(version+"_small.png");
		if(image.getHeight() > SMALL_SIZE || image.getWidth() > SMALL_SIZE){
			image = Scalr.resize(image, SMALL_SIZE);
		}
		fileService.prepareFileCreation(targetFile);
		ImageIO.write(image, "png", targetFile.toFile());
	}
	
	@Override
	public void deletePreview(Path taskFolder) throws IOException {
		Path preview = taskFolder.resolve(config.SUB_PREVIEW);
		Path previewFile;
		previewFile = preview.resolve("newest_full.png");
		if(previewFile.toFile().exists()) {
			fileService.delete(previewFile);
		}
		previewFile = preview.resolve("newest_small.png");
		if(previewFile.toFile().exists()) {
			fileService.delete(previewFile);
		}
	}

	@Override
	public Texture createAsset(Path relative) {
		return new Texture(relative);
	}

	@Override
	public Class<TextureTask> getTaskType() {
		return TextureTask.class;
	}

	@Override
	protected TextureTask createNew(Path assetPath, User user) throws Exception {
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
		String imageName = version + "_" + (small ? "small" : "full") + ".png";		
		
		Path imagePath = config.ASSETS_NEW
				.resolve(task.getAssetPath())
				.resolve(config.SUB_PREVIEW)
				.resolve(imageName);

		if(!imagePath.toFile().exists()) {
			return null;
		}
		BufferedImage image = ImageIO.read(imagePath.toFile());
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(image, "png", out);
		byte[] bytes = out.toByteArray();
		return bytes;
	}
}
