package gmm.service.tasks;

import gmm.domain.AssetTask;
import gmm.service.FileService;
import gmm.service.data.DataConfigService;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;

import org.imgscalr.Scalr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Should maybe be joined with TextureService after Mesh implementation.
 * 
 * @author Jan
 */
@Service
public class TexturePreviewCreator implements PreviewCreator {

	@Autowired DataConfigService config;
	@Autowired FileService fileService;
	
	private static final int SMALL_SIZE = 420;
	
	@PostConstruct
	private void init() {
		//register TGA loader plugin
		IIORegistry registry = IIORegistry.getDefaultInstance();
		registry.registerServiceProvider(new com.realityinteractive.imageio.tga.TGAImageReaderSpi());
	}
	
	/**
	 * Create Preview files from texture file.
	 * For more texture operations see {@link gmm.service.tasks.TextureService}
	 */
	@Override
	public void createPreview(Path sourceFile, AssetTask task, boolean isOriginal) throws IOException {
		if(!sourceFile.toFile().exists()) {
			return;
		}
		Path taskFolder = config.ASSETS_NEW.resolve(task.getNewAssetFolder());
		Path targetFile;
		String version = isOriginal ? "original" : "newest";
		BufferedImage image = ImageIO.read(sourceFile.toFile());
		
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
}
