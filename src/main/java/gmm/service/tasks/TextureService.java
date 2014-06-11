package gmm.service.tasks;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import gmm.domain.AssetTask;
import gmm.service.FileService;
import gmm.service.data.DataConfigService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Should be used for asset operations like asset loading/asset conversion.
 * TODO: When MeshService is done, maybe a partly common Interface with this class is possible.
 * 
 * IF so, this class could be joined with TexturePreviewCreator
 * (as MeshService should be joined with MeshPreviewCreator).
 * 
 * @author Jan
 */
@Service
public class TextureService {

	@Autowired FileService fileService;
	@Autowired DataConfigService config;
	@Autowired TexturePreviewCreator creator;
	
	/**
	 * Returns a texture preview image (png) as byte array.
	 */
	public byte[] getPreview(AssetTask task, boolean small, String version) throws IOException {
		
		String imageName = version + "_" + (small ? "small" : "full") + ".png";		
		
		Path imagePath = config.ASSETS_NEW
				.resolve(task.getNewAssetFolder())
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
	public void addTextureFile(MultipartFile file, AssetTask task) throws IOException {
		
		String relativeFile = file.getOriginalFilename();
		boolean isAsset = relativeFile.endsWith(".tga") || relativeFile.endsWith(".TGA");
		
		//Add file
		Path assetPath = config.ASSETS_NEW
				.resolve(task.getNewAssetFolder())
				.resolve(isAsset ? config.SUB_ASSETS : config.SUB_OTHER)
				.resolve(relativeFile);
		fileService.createFile(assetPath, file.getBytes());
		if(isAsset) task.setNewestAssetName(relativeFile);
		
		//Add texture preview files
		if(isAsset) {
			creator.createPreview(assetPath, task, false);
		}
	}
	
	public void deleteTextureFile(Path relativeFile, boolean isAsset, AssetTask task) throws IOException {
		
		//Restrict access
		Path taskFolder = config.ASSETS_NEW.resolve(task.getNewAssetFolder());
		Path visible = taskFolder.resolve(isAsset ? config.SUB_ASSETS : config.SUB_OTHER);
		Path assetPath = visible.resolve(fileService.restrictAccess(relativeFile, visible));
		
		//Delete previews
		if(isAsset && task.getNewestAssetName()!=null) {
			if(assetPath.getFileName().toString().equals(task.getNewestAssetName())) {
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
				task.setNewestAssetName(null);
			}
		}
		//Delete file
		fileService.delete(assetPath);
	}
}
