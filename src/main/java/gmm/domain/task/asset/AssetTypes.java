package gmm.domain.task.asset;

import java.util.HashMap;
import java.util.Map;

import gmm.domain.task.TaskType;
import gmm.service.FileService.FileExtensionFilter;

/**
 * Static information about asset types like valid file extensions.
 * @author Jan Mothes
 */
public class AssetTypes {
	
	private static final Map<TaskType, AssetType> assetTypes = new HashMap<>();
	
	static {
		assetTypes.put(TaskType.TEXTURE, new AssetType("tga"));
		assetTypes.put(TaskType.MESH, new AssetType("3ds"));
	}
	
	public static class AssetType {
		public final String[] extensions;
		public final FileExtensionFilter extensionsFilter;
		public AssetType(String... extensions) {
			this.extensions = extensions;
			extensionsFilter = new FileExtensionFilter(extensions);
		}
	}
	
	public static AssetType get(TaskType taskType) {
		return assetTypes.get(taskType);
	}
}
