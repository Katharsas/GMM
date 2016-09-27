package gmm.web.sessions;


import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import gmm.collections.Collection;
import gmm.collections.LinkedList;
import gmm.collections.List;
import gmm.domain.User;
import gmm.domain.task.asset.AssetTask;
import gmm.domain.task.asset.ModelTask;
import gmm.domain.task.asset.TextureTask;
import gmm.service.ajax.BundledMessageResponses;
import gmm.service.ajax.ConflictAnswer;
import gmm.service.ajax.MessageResponse;
import gmm.service.ajax.operations.AssetPathConflictCheckerFactory;
import gmm.service.ajax.operations.AssetPathConflictCheckerFactory.AssetPathConflictChecker;
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
	private final AssetPathConflictCheckerFactory assetPathConflictCheckerFactory;
	
	@Autowired
	public AdminSession(TaskServiceFinder taskService,
			AssetPathConflictCheckerFactory assetPathConflictCheckerFactory,
			TaskIdConflictCheckerFactory taskIdConflictCheckerFactory,
			DataAccess data, UserService users) {
		
		super(taskService, assetPathConflictCheckerFactory, taskIdConflictCheckerFactory);
		
		this.taskService = taskService;
		this.assetPathConflictCheckerFactory = assetPathConflictCheckerFactory;
		this.data = data;
		
		loggedInUser = users.getLoggedInUser();
	}
	
	/*--------------------------------------------------
	 * Import asset tasks
	 * ---------------------------------------------------*/
	
	private BundledMessageResponses<String> assetImporter;
	private final List<String> importFilePaths = new LinkedList<>(String.class);
	private boolean areTexturePaths = true;
	
	// Add asset paths
	
	public void addImportPaths(Collection<String> paths, boolean areTexturePaths) {
		if(this.areTexturePaths != areTexturePaths) {
			importFilePaths.clear();
			this.areTexturePaths = areTexturePaths;
		}
		for (final String path : paths) {
			if (!importFilePaths.contains(path)) {
				importFilePaths.add(path);
			}
		}
	}
	public void clearImportPaths() {
		importFilePaths.clear();
	}
	
	public List<String> getImportPaths() {
		return importFilePaths.copy();
	}
	
	// Check asset paths for conflict
	
	public List<MessageResponse> firstImportCheckBundle(TaskForm form) {
		final Class<? extends AssetTask<?>> type = areTexturePaths ?
				TextureTask.class : ModelTask.class;
		
		final Consumer<String> onAssetPathChecked = (assetPath) -> {
			form.setAssetPath(assetPath);
			data.add(taskService.create(type, form, loggedInUser));
		};
		final AssetPathConflictChecker ops =
				assetPathConflictCheckerFactory.create(onAssetPathChecked);
		
		assetImporter = new BundledMessageResponses<>(
				getImportPaths(), ops, ()->{assetImporter = null;});
		
		return assetImporter.firstBundle();
	}
	
	public List<MessageResponse> nextImportCheckBundle(ConflictAnswer answer) {
		return assetImporter.nextBundle(answer);
	}
}
