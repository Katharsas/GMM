package gmm.service.ajax.operations;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.stereotype.Service;

import gmm.domain.task.asset.AssetTask;
import gmm.service.FileService;
import gmm.service.ajax.operations.ConflictChecker.Conflict;
import gmm.service.ajax.operations.ConflictChecker.Operation;
import gmm.service.data.DataAccess;
import gmm.service.data.DataConfigService;

/**
 * Possible conflicts:
 * 1. - task with this assetPath already exists
 * 2. - only new folder exists (not task)
 * 3. - id conflict (not handled in this class, use TaskLoaderOperations)
 * 
 * Choice 1.:
 * - Skip
 * - Aquire data: Delete existing, use data from new asset folder / original folder (sets newestAsset as in deleted task if exists)
 * - Delete: Delete existing, delete new asset folder, use original folder
 * 
 * Choice 2.:
 * - Skip
 * - Aquire data: use data from new asset folder
 * - Delete: Delete new asset folder
 * 
 * @author Jan Mothes
 */
@Service
public class AssetPathConflictCheckerFactory {
	
	private final FileService fileService;
	private final DataConfigService config;
	private final DataAccess data;
	
	public AssetPathConflictCheckerFactory(
			FileService fileService, DataConfigService config, DataAccess data) {
		this.fileService = fileService;
		this.config = config;
		this.data = data;
	}
	
	public AssetPathConflictChecker create(Consumer<String> onAssetPathChecked) {
		return new AssetPathConflictChecker(onAssetPathChecked);
	}
	
	private final Conflict<String> taskConflict = new Conflict<String>() {
		@Override public String getName() {
			return "taskConflict";
		}
		@Override public String getDetails(String assetPath) {
			return "Conflict: There is already a task with path \""+assetPath+"\" !";
		}
	};
	private final Conflict<String> onlyFolderConflict = new Conflict<String>() {
		@Override public String getName() {
			return "folderConflict";
		}
		@Override public String getDetails(String assetPath) {
			return "Conflict: There may already be new data saved for path \""+assetPath+"\" !";
		}
	};
	
	private Map<String, Operation<String>> createOperations(
			AssetPathConflictChecker checker, DataAccess data) {
		final Map<String, Operation<String>> map = new HashMap<>();
		// both
		map.put("skip", (conflict, assetPath) -> {
			checker.assertConflict(conflict.isOneOf(taskConflict, onlyFolderConflict));
			return "Skipping import for conflicting path \""+assetPath+"\"";
		});
		// task conflict
		map.put("overwriteTaskAquireData", (conflict, assetPath) -> {
			checker.assertConflict(conflict.equals(taskConflict));
			
			data.remove(checker.conflictingTask);
			checker.onValidAssetPath(assetPath);
			return "Overwriting existing task and aquiring existing data for path \""+assetPath+"\" !";
		});
		map.put("overwriteTaskDeleteData", (conflict, assetPath) -> {
			checker.assertConflict(conflict.equals(taskConflict));
			
			data.remove(checker.conflictingTask);
			fileService.delete(config.assetsNew().resolve(assetPath));
			checker.onValidAssetPath(assetPath);
			return "Overwriting existing task and deleting existing data for path \""+assetPath+"\" !";
		});
		// only folder conflict
		map.put("aquireData", (conflict, assetPath) -> {
			checker.assertConflict(conflict.equals(onlyFolderConflict));
			
			checker.onValidAssetPath(assetPath);
			return "Aquiring existing data for path \""+assetPath+"\" !";
		});
		map.put("deleteData",(conflict, assetPath) -> {
			checker.assertConflict(conflict.equals(onlyFolderConflict));
			
			fileService.delete(config.assetsNew().resolve(assetPath));
			checker.onValidAssetPath(assetPath);
			return "Deleting existing data for path \""+assetPath+"\" !";
		});
		
		return map;
	}
	
	
	public class AssetPathConflictChecker extends ConflictChecker<String> {

		private final Consumer<String> onAssetPathChecked;
		private final Map<String, Operation<String>> ops;
		
		private AssetTask<?> conflictingTask;
		
		private AssetPathConflictChecker(Consumer<String> onAssetPathChecked) {
			this.onAssetPathChecked = onAssetPathChecked;
			this.ops = createOperations(this, data);
		}
		
		private void onValidAssetPath(String assetPath) {
			onAssetPathChecked.accept(assetPath);
		}
		
		@Override
		public Map<String, Operation<String>> getAllOperations() {
			return ops;
		}
		
		@Override
		public Conflict<String> onLoad(String assetPathString) {
			Path assetPath = Paths.get(assetPathString);
			assetPath = fileService.restrictAccess(assetPath, config.assetsNew());

			// full AssetTask conflict
			for (final AssetTask<?> t : data.getList(AssetTask.class)) {
				if (t.getAssetPath().equals(assetPath)) {
					conflictingTask = t;
					return taskConflict;
				}
			}
			// only conflict with new asset folder
			if (config.assetsNew().resolve(assetPath).toFile().exists()) {
				return onlyFolderConflict;
			}
			return NO_CONFLICT;
		}

		@Override
		public String onDefault(String assetPath) {
			onValidAssetPath(assetPath);
			return "Successfully imported asset at \""+assetPath+"\"";
		}
	}
}
