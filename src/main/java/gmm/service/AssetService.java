package gmm.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import gmm.service.data.DataConfigService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(image, "png", baos);
		byte[] bytes = baos.toByteArray();
		return bytes;
	}
}
