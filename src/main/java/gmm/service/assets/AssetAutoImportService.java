package gmm.service.assets;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gmm.domain.User;
import gmm.domain.task.asset.AssetKey;
import gmm.domain.task.asset.AssetName;
import gmm.domain.task.asset.AssetTask;
import gmm.service.ajax.AutoResponseBundleHandler;
import gmm.service.ajax.ConflictAnswer;
import gmm.service.ajax.operations.AssetNameConflictCheckerFactory;
import gmm.service.ajax.operations.AssetNameConflictCheckerFactory.AssetNameConflictChecker;
import gmm.service.ajax.operations.AssetNameConflictCheckerFactory.OpKey;
import gmm.service.data.DataAccess;
import gmm.service.tasks.AssetTaskService;
import gmm.service.tasks.TaskServiceFinder;
import gmm.web.forms.AssetTaskTemplateForm;
import gmm.web.forms.TaskForm;

@Service
public class AssetAutoImportService {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private final DataAccess data;
	private final AssetService assets;
	private final TaskServiceFinder taskService;
	private final AssetNameConflictCheckerFactory assetNameConflictCheckerFactory;
	
	private final Map<AssetKey, AssetTask<?>> liveAssetTasks;
	
	@Autowired
	public AssetAutoImportService(DataAccess data, AssetService assets, TaskServiceFinder taskService,
			AssetNameConflictCheckerFactory assetNameConflictCheckerFactory) {
		this.data = data;
		this.assets = assets;
		this.taskService = taskService;
		this.assetNameConflictCheckerFactory = assetNameConflictCheckerFactory;
		
		liveAssetTasks = assets.getNewAssetFoldersTaskEvents().getLiveView();
		
		var liveNewAssets = assets.getNewAssetFoldersEvents();
		liveNewAssets.register(this::onAssetAdded, null);
		
		createAllMissingTasks();
	}
	
	public void createAllMissingTasks() {
		if (data.getCombinedData().isTaskAutoImportEnabled()) {
			var newAssets = assets.getNewAssetFoldersWithoutTasks();
			var newAssetNames = newAssets.stream()
					.map(NewAssetFolderInfo::getAssetFolderName)
					.collect(Collectors.toList());
			logger.info("Importing all missing new assets as AssetTasks. Number of assets to import: " + newAssets.size());
			createTasks(newAssetNames);
		}
	}
	
	private void onAssetAdded(AssetKey key, NewAssetFolderInfo folderInfo) {
		if (data.getCombinedData().isTaskAutoImportEnabled()) {
			if (!liveAssetTasks.containsKey(key)) {
				createTasks(List.of(folderInfo.getAssetFolderName()));
			}
		}
	}
	
	private void createTasks(Iterable<AssetName> assetNames) {
		AssetTaskTemplateForm formTemplate = data.getCombinedData().getImportTaskForm();
		
		final Consumer<AssetName> onAssetNameChecked = (assetName) -> {
			final AssetTaskService<?> service = taskService.tryGetAssetService(assetName.getKey());
			if (service != null) {
				TaskForm form = formTemplate.createTaskForm(service.getTaskType(), assetName.get(), true);
				AssetTask<?> task = service.create(form, User.SYSTEM);
				data.add(task);
			} else {
				logger.info("Skipping auto-import for '" + assetName + "' because no matching asset service could be found.");
			}
		};
		final AssetNameConflictChecker ops = assetNameConflictCheckerFactory.create(onAssetNameChecked, false);
		
		final AutoResponseBundleHandler autoLoader = new AutoResponseBundleHandler();		
		
		autoLoader.processResponses(assetNames, ops,  (conflict) -> {
			logger.error("AssetName checker conflict!", new IllegalStateException("Unexpected conflict during automatic task import: " + conflict));
			return new ConflictAnswer<>(OpKey.skip, true);
		});
	}
}
