package gmm.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import gmm.domain.TextureTask;
import gmm.service.data.DataConfigService;

import org.imgscalr.Scalr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Should be used for asset operations like asset loading/asset conversion.
 * TODO: Move tga to png conversion here.
 * 
 * @author Jan
 */
@Service
public class AssetService {

	@Autowired
	FileService fileService;
	
	@Autowired
	DataConfigService config;
	
	private static final int SMALL_SIZE = 420;
	
	/**
	 * Returns a texture preview image (png) as byte array.
	 */
	public byte[] getPreview(String newAssetFolder, boolean small, String version) throws IOException {
		
		String imageName = version + "_" + (small ? "small" : "full") + ".png";
		File imageFile = new File(newAssetFolder+"/"+config.NEW_TEX_PREVIEW, imageName);
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
		String subDir = asset ? config.NEW_TEX_ASSETS : config.NEW_TEX_OTHER;
		
		//Add file
		Path assetPath = Paths.get(task.getNewAssetFolderPath())
				.resolve(subDir)
				.resolve(name);
		fileService.createFile(assetPath, texture.getBytes());
		
		//Add texture preview files
		if(asset) {
			String imageName;
			String dir = task.getNewAssetFolderPath()+"/"+config.NEW_TEX_PREVIEW+"/";
			
			//Full preview
			imageName = "newest_full.png";
			InputStream in = new ByteArrayInputStream(texture.getBytes());
			BufferedImage image = ImageIO.read(in);
			ImageIO.write(image, "png", new File(dir+imageName));
			
			//Small preview
			imageName = "newest_small.png";
			if(image.getHeight() > SMALL_SIZE || image.getWidth() > SMALL_SIZE){
				image = Scalr.resize(image, SMALL_SIZE);
			}
			ImageIO.write(image, "png", new File(dir+imageName));
		}
	}
}
