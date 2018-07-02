package gmm.web.sessions;


import java.nio.file.Path;
import java.util.function.Consumer;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import gmm.collections.ArrayList;
import gmm.collections.Collection;
import gmm.collections.List;
import gmm.domain.User;
import gmm.domain.task.asset.AssetGroupType;
import gmm.domain.task.asset.AssetName;
import gmm.domain.task.asset.AssetTask;
import gmm.service.VirtualNewAssetFileSystem;
import gmm.service.ajax.BundledMessageResponses;
import gmm.service.ajax.ConflictAnswer;
import gmm.service.ajax.MessageResponse;
import gmm.service.ajax.operations.AssetNameConflictCheckerFactory;
import gmm.service.ajax.operations.AssetNameConflictCheckerFactory.AssetNameConflictChecker;
import gmm.service.ajax.operations.TaskIdConflictCheckerFactory;
import gmm.service.assets.AssetService;
import gmm.service.data.DataAccess;
import gmm.service.data.backup.BackupExecutorService;
import gmm.service.data.backup.TaskBackupLoader;
import gmm.service.tasks.AssetTaskService;
import gmm.service.tasks.TaskServiceFinder;
import gmm.service.users.UserService;
import gmm.web.forms.TaskForm;

/**
 * Represents the current state of the asset selection the user has made for asset import.
 * 
 * @author Jan Mothes
 */
@Component
@Scope(value="session", proxyMode=ScopedProxyMode.TARGET_CLASS)
public class AdminSession extends TaskBackupLoader {
	
	private final AssetService assets;
	private final TaskServiceFinder taskService;
	private final BackupExecutorService backups;
	private final DataAccess data;
	
	private final User loggedInUser;
	private final AssetNameConflictCheckerFactory assetNameConflictCheckerFactory;
	
	private VirtualNewAssetFileSystem newAssetsWithoutTasksVfs;
	
	@Autowired
	public AdminSession(TaskServiceFinder taskService,
			AssetService assets,
			BackupExecutorService backups,
			AssetNameConflictCheckerFactory assetNameConflictCheckerFactory,
			TaskIdConflictCheckerFactory taskIdConflictCheckerFactory,
			DataAccess data, UserService users) {
		
		super(data, assetNameConflictCheckerFactory, taskIdConflictCheckerFactory);
		
		this.assets = assets;
		this.taskService = taskService;
		this.backups = backups;
		
		this.assetNameConflictCheckerFactory = assetNameConflictCheckerFactory;
		this.data = data;
		
		loggedInUser = users.getLoggedInUser();
	}
	
	@PostConstruct
	private void init() {
		newAssetsWithoutTasksVfs = new VirtualNewAssetFileSystem(assets.getNewAssetFoldersWithoutTasks());
	}
	
	 public VirtualNewAssetFileSystem getNewAssetsWithoutTasksVfs() {
		 return newAssetsWithoutTasksVfs;
	 }
	
	/*--------------------------------------------------
	 * Import asset tasks
	 * ---------------------------------------------------*/
	
	private BundledMessageResponses<AssetName> assetImporter;
	private final List<Path> importFilePaths = new ArrayList<>(Path.class);
	private AssetGroupType type = AssetGroupType.ORIGINAL;
	private Collection<AssetTask<?>> importedTasks;
	
	// Add asset paths
	
	public void addImportPaths(Collection<Path> paths, AssetGroupType type) {
		if(this.type != type) {
			importFilePaths.clear();
			this.type = type;
		}
		for (final Path path : paths) {
			if (!importFilePaths.contains(path)) {
				importFilePaths.add(path);
			}
		}
	}
	public void clearImportPaths() {
		importFilePaths.clear();
	}
	
	public List<Path> getImportPaths() {
		return importFilePaths.copy();
	}
	
	// Check asset filenames for conflict
	
	public List<MessageResponse> firstImportCheckBundle(TaskForm form) {
		
		importedTasks = new ArrayList<>(AssetTask.getGenericClass(), importFilePaths.size());
		
		final Consumer<AssetName> onAssetNameChecked = (assetName) -> {
			form.setAssetName(assetName.get());
			final AssetTaskService<?> service = taskService.getAssetService(assetName);
			form.setType(service.getTaskType());
			importedTasks.add(service.create(form, loggedInUser));
		};
		final AssetNameConflictChecker ops =
				assetNameConflictCheckerFactory.create(onAssetNameChecked, true);
		
		final List<AssetName> fileNames = new ArrayList<>(AssetName.class, importFilePaths.size());
		for (final Path path : importFilePaths) {
			fileNames.add(new AssetName(path));
		}
		
		assetImporter = new BundledMessageResponses<>(
				fileNames, ops, ()->{
					assetImporter = null;
					data.addAll(importedTasks);
					newAssetsWithoutTasksVfs.update();
					backups.triggerTaskBackup(false);
		});
		
		return assetImporter.firstBundle();
	}
	
	public List<MessageResponse> nextImportCheckBundle(ConflictAnswer answer) {
		return assetImporter.nextBundle(answer);
	}
}
