package gmm.domain.task.asset;

import org.springframework.beans.factory.annotation.Autowired;

import gmm.collections.ArrayList;
import gmm.collections.List;
import gmm.service.tasks.AssetTaskService;

public class AssetTypeService {
	
	private List<? extends AssetTaskService<?>> list;
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Autowired
	public AssetTypeService(java.util.List<AssetTaskService<?>> assetTaskServices) {
		list = new ArrayList<>((Class) AssetTaskService.class, assetTaskServices);
	}
	
	public List<? extends AssetTaskService<?>> getAssetTaskServices() {
		return list;
	}
	
//	public AssetTaskService<?> getAssetTaskService(String assetTypeFolderName) {
//		for (AssetTaskService<?> assetTaskService : list) {
//			String expected = assetTaskService.getAssetTypeSubFolder().toFile().getName();
//			if (expected.equalsIgnoreCase(assetTypeFolderName)) {
//				return assetTaskService;
//			}
//		}
//		throw new IllegalArgumentException(
//				"Could not find service for folder '" + assetTypeFolderName + "'!");
//	}
}