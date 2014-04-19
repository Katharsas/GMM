package gmm.service;

import java.io.File;

import gmm.service.data.DataConfigService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AssetService {

	@Autowired
	DataConfigService service;
	
	public File linkNewAssetFolder(String newAssetFolderPath) {
		boolean success = true;
		
		File result = new File(newAssetFolderPath);
		if(!result.exists()) {
			success = success && result.mkdirs();
		}
		success = success && createNewAssetSubFolder(result, service.NEW_TEX_ASSETS);
		success = success && createNewAssetSubFolder(result, service.NEW_TEX_PREVIEW);
		success = success && createNewAssetSubFolder(result, service.NEW_TEX_OTHER);
		if(!success) {
			throw new IllegalStateException("Could not create folders for new texture asset at "+newAssetFolderPath+"!");
		}
		return result;
	}
	
	private boolean createNewAssetSubFolder(File newAssetFolder, String folderName){
		File subFolder = new File(newAssetFolder.getPath()+"/"+folderName);
		return subFolder.exists() ? true : subFolder.mkdir();
	}
}
