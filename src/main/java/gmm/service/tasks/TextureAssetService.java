package gmm.service.tasks;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import gmm.domain.Texture;
import gmm.domain.TextureTask;
import gmm.service.FileService;
import gmm.service.data.DataConfigService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Should be used for asset operations like asset loading/asset conversion.
 * TODO: When MeshAssetService is done, maybe a partly common Interface with this class is possible.
 * 
 * If so, this class could be joined with {@link TextureTaskService}
 * (as MeshService should be joined with {@link ModelTaskService}).
 * 
 * @author Jan
 */
@Service
public class TextureAssetService {

	@Autowired private FileService fileService;
	@Autowired private DataConfigService config;
	@Autowired private TextureTaskService creator;
	
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
	
	/**
	 * Called when new task file is uploaded.
	 */
	public void addTextureFile(MultipartFile file, TextureTask task) throws IOException {
		
		String fileName = file.getOriginalFilename();
		boolean isAsset = fileName.endsWith(".tga") || fileName.endsWith(".TGA");
		
		//Add file
		Path relative = task.getAssetPath()
				.resolve(isAsset ? config.SUB_ASSETS : config.SUB_OTHER)
				.resolve(fileName);
		Path assetPath = config.ASSETS_NEW.resolve(relative);
		fileService.createFile(assetPath, file.getBytes());
		
		if(isAsset) {
			task.setNewestAsset(creator.createAsset(Paths.get(fileName)));
			creator.createPreview(assetPath, task, false);
		}
	}
	
	public void deleteTextureFile(Path relativeFile, boolean isAsset, TextureTask task) throws IOException {
		
		//Restrict access
		Path taskFolder = config.ASSETS_NEW.resolve(task.getAssetPath());
		Path visible = taskFolder.resolve(isAsset ? config.SUB_ASSETS : config.SUB_OTHER);
		Path assetPath = visible.resolve(fileService.restrictAccess(relativeFile, visible));
		
		//Delete previews
		Texture tex = task.getNewestAsset();
		if(isAsset && tex!=null) {
			if(assetPath.getFileName().toString().equals(tex.getFileName())) {
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
				task.setNewestAsset(null);
			}
		}
		//Delete file
		fileService.delete(assetPath);
	}
}
