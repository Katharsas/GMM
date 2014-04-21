package gmm.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import gmm.service.data.DataConfigService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AssetService {

	@Autowired
	DataConfigService config;
	
	public File linkNewAssetFolder(String newAssetFolderPath) {
		boolean success = true;
		
		File result = new File(newAssetFolderPath);
		if(!result.exists()) {
			success = success && result.mkdirs();
		}
		success = success && createNewAssetSubFolder(result, config.NEW_TEX_ASSETS);
		success = success && createNewAssetSubFolder(result, config.NEW_TEX_PREVIEW);
		success = success && createNewAssetSubFolder(result, config.NEW_TEX_OTHER);
		if(!success) {
			throw new IllegalStateException("Could not create folders for new texture asset at "+newAssetFolderPath+"!");
		}
		return result;
	}
	
	private boolean createNewAssetSubFolder(File newAssetFolder, String folderName){
		File subFolder = new File(newAssetFolder.getPath()+"/"+folderName);
		return subFolder.exists() ? true : subFolder.mkdir();
	}
	
	public byte[] getPreview(String newAssetFolder) throws IOException {
		File imageFile = new File(newAssetFolder+"/"+config.NEW_TEX_PREVIEW, "preview.png");
		BufferedImage image = ImageIO.read(imageFile);
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(image, "png", baos);
		baos.flush();
		byte[] bytes = baos.toByteArray();
		baos.close();
		return bytes;
		
//		return ((DataBufferByte) image.getData().getDataBuffer()).getData();
//		return ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
	}

}
