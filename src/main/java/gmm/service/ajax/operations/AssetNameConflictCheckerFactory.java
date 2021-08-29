package gmm.service.ajax.operations;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.stereotype.Service;

import gmm.domain.task.asset.AssetName;
import gmm.domain.task.asset.AssetTask;
import gmm.service.ajax.operations.ConflictChecker.Conflict;
import gmm.service.ajax.operations.ConflictChecker.Operation;
import gmm.service.data.DataAccess;
import gmm.util.TypedString;

/**
 * @author Jan Mothes
 */
@Service
public class AssetNameConflictCheckerFactory {
	
	public static enum ConflictKey {
		autoSkipConflict, fileNameConflict
	}
	
	public static class OpKey extends TypedString {
		public final static OpKey skip = new OpKey("skip");
		public final static OpKey overwrite = new OpKey("overwrite");
		
		private OpKey(String name) {
			super(name);
		}
	}
	
	private final DataAccess data;
	
	public AssetNameConflictCheckerFactory(DataAccess data) {
		this.data = data;
	}
	
	public AssetNameConflictChecker create(Consumer<AssetName> onAssetNameChecked, boolean skipIfNameConflict) {
		return new AssetNameConflictChecker(onAssetNameChecked, skipIfNameConflict);
	}
	
	private final Conflict<AssetName> autoSkipConflict = new Conflict<AssetName>() {
		@Override public String getName() {
			return "autoSkipConflict";
		}
		@Override public String getDetails(AssetName assetPath) {
			return "Info: Task with asset filename \""+assetPath+"\" exists already, skipping import!";
		}
	};
	
	private final Conflict<AssetName> fileNameConflict = new Conflict<AssetName>() {
		@Override public String getName() {
			return "taskConflict";
		}
		@Override public String getDetails(AssetName assetPath) {
			return "Conflict: There is already an asset task with asset filename \""+assetPath+"\" !";
		}
	};
	
	private Map<OpKey, Operation<AssetName>> createOperations(
			AssetNameConflictChecker checker, DataAccess data) {
		final Map<OpKey, Operation<AssetName>> map = new HashMap<>();
		// both
		map.put(OpKey.skip, (conflict, assetName) -> {
			checker.assertConflict(conflict.equals(fileNameConflict) || conflict.equals(autoSkipConflict));
			
			return "Skipping this task for conflicting filename \""+assetName+"\"";
		});
		// task conflict
		map.put(OpKey.overwrite, (conflict, assetName) -> {
			checker.assertConflict(conflict.equals(fileNameConflict));
			
			data.remove(checker.conflictingTask);
			checker.onValidAssetName(assetName);
			return "Overwriting existing task for conflicting filename \""+assetName+"\" !";
		});
		
		return map;
	}
	
	public class AssetNameConflictChecker extends ConflictChecker<AssetName, OpKey> {

		private final boolean skipIfNameConflict;
		
		private final Consumer<AssetName> onAssetNameChecked;
		private final Map<OpKey, Operation<AssetName>> ops;
		
		private AssetTask<?> conflictingTask;
	
		private AssetNameConflictChecker(Consumer<AssetName> onAssetNameChecked, boolean skipIfNameConflict) {
			super(OpKey::new);
			this.skipIfNameConflict = skipIfNameConflict;
			this.onAssetNameChecked = onAssetNameChecked;
			this.ops = createOperations(this, data);
		}
		
		private void onValidAssetName(AssetName assetPath) {
			onAssetNameChecked.accept(assetPath);
		}
		
		@Override
		public Map<OpKey, Operation<AssetName>> getAllOperations() {
			return ops;
		}
		
		@Override
		public Conflict<AssetName> onLoad(AssetName assetName) {
			
			for (final AssetTask<?> t : data.getList(AssetTask.class)) {
				if (t.getAssetName().equals(assetName)) {
					conflictingTask = t;
					return skipIfNameConflict ? autoSkipConflict : fileNameConflict;
				}
			}
			return NO_CONFLICT;
		}

		@Override
		public String onDefault(AssetName assetPath) {
			onValidAssetName(assetPath);
			return "Successfully imported asset \""+assetPath+"\"";
		}
	}
}
