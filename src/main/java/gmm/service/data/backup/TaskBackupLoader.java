package gmm.service.data.backup;

import java.util.HashMap;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import gmm.collections.ArrayList;
import gmm.collections.Collection;
import gmm.collections.HashSet;
import gmm.domain.task.GeneralTask;
import gmm.domain.task.Task;
import gmm.domain.task.asset.AssetName;
import gmm.domain.task.asset.AssetTask;
import gmm.domain.task.asset.ModelTask;
import gmm.domain.task.asset.TextureTask;
import gmm.service.ajax.BundledMessageResponses;
import gmm.service.ajax.operations.AssetNameConflictCheckerFactory;
import gmm.service.ajax.operations.AssetNameConflictCheckerFactory.AssetNameConflictChecker;
import gmm.service.ajax.operations.TaskIdConflictCheckerFactory;
import gmm.service.ajax.operations.TaskIdConflictCheckerFactory.TaskIdConflictChecker;
import gmm.service.data.DataAccess;

/**
 * Provides methods to load a collection of tasks of arbitrary subtype,
 * usually coming from a task backup file with full conflict management.<br>
 * <br>
 * Usage:<br><ul>
 * <li>call {@link #prepareLoadTasks(Collection)}</li>
 * <li>Call {@link #firstAssetPathCheckBundle()} to start AssetPath conflict checking.</li>
 * <li>Use {@link #nextLoadCheckBundle(String, boolean)} to repeat until success.</li>
 * <li>Call {@link #firstTaskIdCheckBundle()} to start Task ID conflict checking.</li>
 * <li>Again use {@link #nextLoadCheckBundle(String, boolean)} to repeat until success.</li>
 * </ul>
 * 
 * @author Jan Mothes
 */
@Service
@Primary
@Scope("prototype")
public class TaskBackupLoader {
	
	private final DataAccess data;
	private final AssetNameConflictCheckerFactory assetPathConflictCheckerFactory;
	private final TaskIdConflictCheckerFactory taskIdConflictCheckerFactory;
	
	@Autowired
	public TaskBackupLoader(
			DataAccess data,
			AssetNameConflictCheckerFactory assetPathConflictCheckerFactory,
			TaskIdConflictCheckerFactory taskIdConflictCheckerFactory) {
		this.data = data;
		this.assetPathConflictCheckerFactory = assetPathConflictCheckerFactory;
		this.taskIdConflictCheckerFactory = taskIdConflictCheckerFactory;
	}

	private BundledMessageResponses<? extends Task, TaskIdConflictCheckerFactory.OpKey> generalTaskLoader;
	private BundledMessageResponses<AssetName, AssetNameConflictCheckerFactory.OpKey> assetTaskLoader;
	
	private Multimap<Class<? extends Task>, Task> multiMap;
	private Collection<AssetTask<?>> assetImportChecked;
	private boolean isAssetImportCheckDone;
	
	private Collection<Task> fullyChecked;
	
	public void prepareLoadTasks(Collection<Task> tasks) {
		// remove duplicates
		final HashSet<Task> tasksClean = new HashSet<>(tasks);
		// split tasks into types to use correct conflict checker for loading
		multiMap = HashMultimap.create();
		StreamSupport.stream(tasksClean.spliterator(), false)
			.forEach(task -> multiMap.put(task.getClass(), task));
		assetImportChecked = new ArrayList<AssetTask<?>>(AssetTask.getGenericClass(), tasksClean.size());
		isAssetImportCheckDone = false;
		fullyChecked = new ArrayList<>(Task.class, tasksClean.size());
	}
	
	public boolean isFirstLoaderDone() {
		return isAssetImportCheckDone;
	}
	
	public BundledMessageResponses<AssetName, AssetNameConflictCheckerFactory.OpKey> getFirstLoader() {
		if (isAssetImportCheckDone == false && assetTaskLoader == null) {
			createAssetTaskLoader();
		}
		Objects.requireNonNull(assetTaskLoader);
		return assetTaskLoader;
	}
	
	public BundledMessageResponses<? extends Task, TaskIdConflictCheckerFactory.OpKey> getSecondLoader() {
		if (isAssetImportCheckDone == true && generalTaskLoader == null) {
			createGeneralTaskLoader();
		}
		Objects.requireNonNull(generalTaskLoader);
		return generalTaskLoader;
	}
	
	public void createAssetTaskLoader() {
		final Collection<Task> assetTasks = new HashSet<>(Task.class);
		assetTasks.addAll(multiMap.get(TextureTask.class));
		assetTasks.addAll(multiMap.get(ModelTask.class));
		
		final HashMap<AssetName, AssetTask<?>> assetNameToTask = new HashMap<>();
		for(final Task task : assetTasks) {
			final AssetTask<?> assetTask = (AssetTask<?>) task;
			assetNameToTask.put(assetTask.getAssetName(), assetTask);
		}
		final Consumer<AssetName> onAssetNameChecked = (assetPath) -> {
			final AssetTask<?> task = assetNameToTask.get(assetPath);
			assetImportChecked.add(task);
		};
		final AssetNameConflictChecker ops =
				assetPathConflictCheckerFactory.create(onAssetNameChecked, false);
		
		final Runnable onAssetImportCheckDone = () -> {
			assetTaskLoader = null;
			isAssetImportCheckDone = true;
		};
		assetTaskLoader = new BundledMessageResponses<>(assetNameToTask.keySet(), ops, onAssetImportCheckDone);
	}
	
	public void createGeneralTaskLoader() {
		if(!isAssetImportCheckDone) {
			throw new IllegalStateException(
					"This method cannot be called until assetPath conflict checking has been completed!");
		}
		final java.util.Collection<Task> tasks = multiMap.get(GeneralTask.class);
		tasks.addAll(assetImportChecked);
		
		final TaskIdConflictChecker ops = taskIdConflictCheckerFactory.create(task -> fullyChecked.add(task));
		
		final Runnable onAllChecksDone = () -> {
			generalTaskLoader = null;
			data.addAll(fullyChecked);
			isAssetImportCheckDone = false;
		};
		generalTaskLoader = new BundledMessageResponses<>(tasks, ops, onAllChecksDone);
	}
}
