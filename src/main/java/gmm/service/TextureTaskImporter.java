package gmm.service;

import gmm.domain.TextureTask;
import gmm.domain.User;
import gmm.service.data.DataAccess;
import gmm.service.data.DataConfigService;
import gmm.service.forms.TaskFacade;
import gmm.util.HashSet;
import gmm.util.Set;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TextureTaskImporter {
	
	@Autowired
	DataConfigService config;
	
	@Autowired
	AssetService assetService;
	
	@Autowired
	DataAccess data;
	
	public TextureTaskImporter() {
		//register TGA loader plugin
		IIORegistry registry = IIORegistry.getDefaultInstance();
		registry.registerServiceProvider(new com.realityinteractive.imageio.tga.TGAImageReaderSpi());
	}
	
	public void importTasks(Iterable<String> texturePaths, TaskFacade form, User user) {
		Set<TextureTask> result = new HashSet<>();
		for(String path : texturePaths) {
			result.add(importTask(Paths.get(path), user));
		}
		data.addAll(TextureTask.class, result);
	}
	
	/**
	 * TODO a lot
	 */
	public TextureTask importTask(Path path, User user) {
		path = path.toAbsolutePath();
		
		TextureTask result;
		try {
			//get relative path and new folder path
			Path base = Paths.get(config.ASSETS_ORIGINAL).toAbsolutePath();
			Path relPath = base.relativize(path.toAbsolutePath());
			Path newFolder = Paths.get(config.ASSETS_NEW).resolve(relPath).toAbsolutePath();
			System.out.println("Creating new asset at: "+newFolder);
			
			//create Task
			result = new TextureTask(""+path.getFileName(), user);
			result.setAssetFolderPaths(""+path, ""+newFolder);
			result.updateAssetAccess(assetService);
			
			//generate previews
			BufferedImage image = ImageIO.read(path.toFile());
			Path previewPath = newFolder.resolve(config.NEW_TEX_PREVIEW).resolve("preview.png");
			ImageIO.write(image, "png", previewPath.toFile());
		}
		catch (IOException e) {
			throw new IllegalStateException(e.getMessage(), e.getCause());
		}
		
		return result;
	}
}
