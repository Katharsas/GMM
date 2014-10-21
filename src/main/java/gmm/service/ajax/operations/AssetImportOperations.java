package gmm.service.ajax.operations;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import gmm.domain.AssetTask;
import gmm.service.FileService;
import gmm.service.Spring;
import gmm.service.data.DataAccess;
import gmm.service.data.DataConfigService;
import gmm.service.tasks.TaskServiceFinder;
import gmm.web.forms.TaskForm;

/**
 * Possible conflicts:
 * 1. - task with this assetPath already exists
 * 2. - only new folder exists (not task)
 * 
 * Choice 1.:
 * - Skip
 * - Aquire data: Delete existing, use data from new asset folder / original folder
 * - Delete: Delete existing, delete new asset folder, use original folder
 * 
 * Choice 2.:
 * - Skip
 * - Aquire data: use data from new asset folder
 * - Delete: Delete new asset folder
 * 
 * @author Jan Mothes
 */
public class AssetImportOperations<E extends AssetTask<?>> extends MessageResponseOperations<String> {

	private final FileService fileService = Spring.get(FileService.class);
	private final DataConfigService config = Spring.get(DataConfigService.class);
	private final DataAccess data = Spring.get(DataAccess.class);
	private final TaskServiceFinder creator = Spring.get(TaskServiceFinder.class);
	
	private final Class<E> clazz;
	private final TaskForm form;
	private AssetTask<?> conflictingTask;
	
	public AssetImportOperations(TaskForm form, Class<E> clazz) {
		this.form = form;
		this.clazz = clazz;
	}
	
	private final Conflict<String> taskConflict = new Conflict<String>() {
		@Override public String getStatus() {
			return "taskConflict";
		}
		@Override public String getMessage(String assetPath) {
			return "Conflict: There is already a task with path \""+assetPath+"\" !";
		}
	};
	private final Conflict<String> onlyFolderConflict = new Conflict<String>() {
		@Override public String getStatus() {
			return "folderConflict";
		}
		@Override public String getMessage(String assetPath) {
			return "Conflict: There may already be new data saved for path \""+assetPath+"\" !";
		}
	};

	@Override
	public Map<String, Operation<String>> getOperations() {
		Map<String, Operation<String>> map = new HashMap<>();
		map.put("skip", new Operation<String>() {
			@Override public String execute(String assetPath) {
				return "Skipping import for conflicting path \""+assetPath+"\"";
			}
		});
		map.put("overwriteTaskAquireData", new Operation<String>() {
			@Override public String execute(String assetPath) throws Exception {
				data.remove(conflictingTask);
				data.add(creator.create(clazz, form));
				return "Overwriting existing task and aquiring existing data for path \""+assetPath+"\" !";
			}
		});
		map.put("overwriteTaskDeleteData", new Operation<String>() {
			@Override public String execute(String assetPath) throws Exception {
				data.remove(conflictingTask);
				fileService.delete(config.ASSETS_NEW.resolve(assetPath));
				data.add(creator.create(clazz, form));
				return "Overwriting existing task and deleting existing data for path \""+assetPath+"\" !";
			}
		});
		map.put("aquireData", new Operation<String>() {
			@Override public String execute(String assetPath) throws Exception {
				data.add(creator.create(clazz, form));
				return "Aquiring existing data for path \""+assetPath+"\" !";
			}
		});
		map.put("deleteData", new Operation<String>() {
			@Override public String execute(String assetPath) throws Exception {
				fileService.delete(config.ASSETS_NEW.resolve(assetPath));
				data.add(creator.create(clazz, form));
				return "Deleting existing data for path \""+assetPath+"\" !";
			}
		});
		return map;
	}
	
	@Override
	public Conflict<String> onLoad(String assetPathString) {
		Path assetPath = Paths.get(assetPathString);
		assetPath = fileService.restrictAccess(assetPath, config.ASSETS_NEW);

		// full AssetTask conflict
		for (AssetTask<?> t : data.getList(AssetTask.class)) {
			if (t.getAssetPath().equals(assetPath)) {
				conflictingTask = t;
				return taskConflict;
			}
		}
		// only conflict with new asset folder
		if (config.ASSETS_NEW.resolve(assetPath).toFile().exists()) {
			return onlyFolderConflict;
		}
		
		return MessageResponseOperations.cast(NO_CONFLICT);
	}

	@Override
	public String onDefault(String assetPath) {
		return "Successfully importet asset at \""+assetPath+"\"";
	}
}
