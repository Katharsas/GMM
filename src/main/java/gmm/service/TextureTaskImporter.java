package gmm.service;

import gmm.domain.TextureTask;
import gmm.domain.User;
import gmm.service.data.DataAccess;
import gmm.service.data.DataConfigService;
import gmm.service.forms.TaskFacade;
import gmm.util.HashSet;
import gmm.util.Set;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;

import org.imgscalr.Scalr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TextureTaskImporter {
	
	@Autowired
	DataConfigService config;
	@Autowired
	AssetService assetService;
	@Autowired
	FileService fileService;
	@Autowired
	DataAccess data;
	
	private static final int SMALL_SIZE = 420;
	
	public TextureTaskImporter() {
		//register TGA loader plugin
		IIORegistry registry = IIORegistry.getDefaultInstance();
		registry.registerServiceProvider(new com.realityinteractive.imageio.tga.TGAImageReaderSpi());
	}
	
	public void importTasks(String root, Iterable<String> texturePaths, TaskFacade form, User user) throws IOException {
		Set<TextureTask> result = new HashSet<>();
		for(String path : texturePaths) {
			result.add(importTask(Paths.get(root).resolve(path), user));
		}
		data.addAll(TextureTask.class, result);
	}
	
	public TextureTask importTask(Path path, User user) throws IOException {
		path = path.toAbsolutePath();
		
		TextureTask result;
		
		//get relative path and new folder path
		Path base = Paths.get(config.ASSETS_ORIGINAL).toAbsolutePath();
		Path relPath = base.relativize(path.toAbsolutePath());
		Path newFolder = Paths.get(config.ASSETS_NEW).resolve(relPath).toAbsolutePath();
		
		//create Task
		result = new TextureTask(""+path.getFileName(), user);
		result.setAssetFolderPaths(""+path, ""+newFolder);
		
		
		
		//generate original previews
		Path previewPath;
		File previewFile;
		BufferedImage image = ImageIO.read(path.toFile());
		
		//full preview
		previewPath = newFolder.resolve(config.NEW_TEX_PREVIEW).resolve("original_full.png");
		previewFile = previewPath.toFile();
		if(!previewFile.exists()) {
			fileService.prepareFileCreation(previewFile.toPath());
			ImageIO.write(image, "png", previewFile);
		}
		//small preview
		previewPath = newFolder.resolve(config.NEW_TEX_PREVIEW).resolve("original_small.png");
		previewFile = previewPath.toFile();
		if(!previewFile.exists()) {
			if(image.getHeight() > SMALL_SIZE || image.getWidth() > SMALL_SIZE){
				image = Scalr.resize(image, SMALL_SIZE);
			}
			fileService.prepareFileCreation(previewFile.toPath());
			ImageIO.write(image, "png", previewFile);
		}
		
		return result;
	}
}
