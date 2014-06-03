package gmm.service.assets;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import gmm.domain.TextureTask;
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
	public byte[] getPreview(String newAssetFolder, boolean small, String version) throws IOException {
		
		String imageName = version + "_" + (small ? "small" : "full") + ".png";
		File imageFile = new File(newAssetFolder+"/"+config.SUB_PREVIEW, imageName);
		if(!imageFile.exists()) {
			return null;
		}
		BufferedImage image = ImageIO.read(imageFile);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(image, "png", out);
		byte[] bytes = out.toByteArray();
		return bytes;
	}
	
	public void addTextureFile(MultipartFile texture, TextureTask task) throws IOException {
		
		String name = texture.getOriginalFilename();
		boolean asset = name.endsWith(".tga") || name.endsWith(".TGA");
		String subDir = asset ? config.SUB_ASSETS : config.SUB_OTHER;
		
		//Add file
		Path assetPath = Paths.get(task.getNewAssetFolderPath())
				.resolve(subDir)
				.resolve(name);
		fileService.createFile(assetPath, texture.getBytes());
		if(asset) task.setNewestAssetName(name);
		
		//Add texture preview files
		if(asset) {
			creator.createPreview(assetPath, Paths.get(task.getNewAssetFolderPath()), false);
		}
	}
	
	public void deleteTextureFile(Path dir, boolean asset, TextureTask task) throws IOException {
		
		String subDir = asset ? config.SUB_ASSETS : config.SUB_OTHER;
		
		//Restrict access
		Path visible = Paths.get(task.getNewAssetFolderPath()).resolve(subDir);
		dir = visible.resolve(fileService.restrictAccess(dir, visible));
		
		//Delete previews
		if(asset && task.getNewestAssetName()!=null) {
			if(dir.getFileName().toString().equals(task.getNewestAssetName())) {
				Path preview = Paths.get(task.getNewAssetFolderPath()).resolve(config.SUB_PREVIEW);
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
		fileService.delete(dir);
	}
}
