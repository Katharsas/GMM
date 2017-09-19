package gmm.web.sessions;


import java.nio.file.Path;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import gmm.collections.ArrayList;
import gmm.collections.Collection;
import gmm.collections.LinkedList;
import gmm.collections.List;
import gmm.domain.User;
import gmm.domain.task.asset.AssetGroupType;
import gmm.domain.task.asset.AssetName;
import gmm.domain.task.asset.AssetTask;
import gmm.domain.task.asset.ModelTask;
import gmm.domain.task.asset.TextureTask;
import gmm.service.ajax.BundledMessageResponses;
import gmm.service.ajax.ConflictAnswer;
import gmm.service.ajax.MessageResponse;
import gmm.service.ajax.operations.AssetNameConflictCheckerFactory;
import gmm.service.ajax.operations.AssetNameConflictCheckerFactory.AssetNameConflictChecker;
import gmm.service.ajax.operations.TaskIdConflictCheckerFactory;
import gmm.service.data.DataAccess;
import gmm.service.data.backup.TaskBackupLoader;
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
	
	private final TaskServiceFinder taskService;
	private final DataAccess data;
	
	private final User loggedInUser;
	private final AssetNameConflictCheckerFactory assetNameConflictCheckerFactory;
	
	@Autowired
	public AdminSession(TaskServiceFinder taskService,
			AssetNameConflictCheckerFactory assetNameConflictCheckerFactory,
			TaskIdConflictCheckerFactory taskIdConflictCheckerFactory,
			DataAccess data, UserService users) {
		
		super(assetNameConflictCheckerFactory, taskIdConflictCheckerFactory);
		
		this.taskService = taskService;
		this.assetNameConflictCheckerFactory = assetNameConflictCheckerFactory;
		this.data = data;
		
		loggedInUser = users.getLoggedInUser();
	}
	
	/*--------------------------------------------------
	 * Import asset tasks
	 * ---------------------------------------------------*/
	
	private BundledMessageResponses<AssetName> assetImporter;
	private final List<Path> importFilePaths = new LinkedList<>(Path.class);
	private boolean areTexturePaths = true;
	private AssetGroupType type = AssetGroupType.ORIGINAL;
	
	// Add asset paths
	
	public void addImportPaths(Collection<Path> paths, boolean areTexturePaths, AssetGroupType type) {
		if(this.type != type) {
			importFilePaths.clear();
			this.type = type;
		}
		if(this.areTexturePaths != areTexturePaths) {
			importFilePaths.clear();
			this.areTexturePaths = areTexturePaths;
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
		final Class<? extends AssetTask<?>> type = areTexturePaths ?
				TextureTask.class : ModelTask.class;
		
		final Consumer<AssetName> onAssetNameChecked = (assetName) -> {
			form.setAssetName(assetName.get());
			data.add(taskService.create(type, form, loggedInUser));
		};
		final AssetNameConflictChecker ops =
				assetNameConflictCheckerFactory.create(onAssetNameChecked);
		
		final List<AssetName> fileNames = new ArrayList<>(AssetName.class, importFilePaths.size());
		for (final Path path : importFilePaths) {
			fileNames.add(new AssetName(path));
		}
		
		assetImporter = new BundledMessageResponses<>(
				fileNames, ops, ()->{assetImporter = null;});
		
		return assetImporter.firstBundle();
	}
	
	public List<MessageResponse> nextImportCheckBundle(ConflictAnswer answer) {
		return assetImporter.nextBundle(answer);
	}
}
