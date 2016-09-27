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

import gmm.collections.Collection;
import gmm.collections.HashSet;
import gmm.collections.LinkedList;
import gmm.collections.List;
import gmm.domain.task.GeneralTask;
import gmm.domain.task.Task;
import gmm.domain.task.asset.Asset;
import gmm.domain.task.asset.AssetGroupType;
import gmm.domain.task.asset.AssetTask;
import gmm.domain.task.asset.ModelTask;
import gmm.domain.task.asset.TextureTask;
import gmm.service.ajax.BundledMessageResponses;
import gmm.service.ajax.BundledMessageResponsesProducer;
import gmm.service.ajax.ConflictAnswer;
import gmm.service.ajax.MessageResponse;
import gmm.service.ajax.operations.AssetPathConflictCheckerFactory;
import gmm.service.ajax.operations.AssetPathConflictCheckerFactory.AssetPathConflictChecker;
import gmm.service.ajax.operations.TaskIdConflictCheckerFactory;
import gmm.service.ajax.operations.TaskIdConflictCheckerFactory.TaskIdConflictChecker;
import gmm.service.tasks.AssetTaskService;
import gmm.service.tasks.TaskServiceFinder;
import gmm.util.Util;

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
	
	private final TaskServiceFinder serviceFinder;
	private final AssetPathConflictCheckerFactory assetPathConflictCheckerFactory;
	private final TaskIdConflictCheckerFactory taskIdConflictCheckerFactory;
	
	@Autowired
	public TaskBackupLoader(TaskServiceFinder serviceFinder,
			AssetPathConflictCheckerFactory assetPathConflictCheckerFactory,
			TaskIdConflictCheckerFactory taskIdConflictCheckerFactory) {
		this.serviceFinder = serviceFinder;
		this.assetPathConflictCheckerFactory = assetPathConflictCheckerFactory;
		this.taskIdConflictCheckerFactory = taskIdConflictCheckerFactory;
	}

	private BundledMessageResponses<? extends Task> generalTaskLoader;
	private BundledMessageResponses<String> assetTaskLoader;
	
	private Multimap<Class<? extends Task>, Task> multiMap;
	private Collection<AssetTask<?>> assetImportChecked;
	private boolean isAssetImportCheckDone;
	
	public void prepareLoadTasks(Collection<Task> tasks) {
		// split tasks into types to use correct conflict checker for loading
		multiMap = HashMultimap.create();
		StreamSupport.stream(tasks.spliterator(), false)
			.forEach(task -> multiMap.put(task.getClass(), task));
		assetImportChecked = new LinkedList<AssetTask<?>>(AssetTask.getGenericClass());
		isAssetImportCheckDone = false;
	}
	
	public BundledMessageResponsesProducer getBundledMessageResponses() {
		return new BundledMessageResponsesProducer() {
			@Override
			public List<MessageResponse> firstBundle() {
				return isAssetImportCheckDone ?
						firstTaskIdCheckBundle() : firstAssetPathCheckBundle();
			}
			@Override
			public List<MessageResponse> nextBundle(ConflictAnswer answer) {
				return nextCheckBundle(answer);
			}
		};
	}
	
	public List<MessageResponse> firstAssetPathCheckBundle() {
		final Collection<Task> assetTasks = new HashSet<>(Task.class);
		assetTasks.addAll(multiMap.get(TextureTask.class));
		assetTasks.addAll(multiMap.get(ModelTask.class));
		
		final HashMap<String, AssetTask<?>> pathToTask = new HashMap<>();
		for(final Task task : assetTasks) {
			final AssetTask<?> assetTask = (AssetTask<?>) task;
			pathToTask.put(assetTask.getAssetPath().toString(), assetTask);
		}
		final Consumer<String> onAssetPathChecked = (assetPath) -> {
			final AssetTask<?> task = pathToTask.get(assetPath);
			task.onLoad();
			final AssetTask<?> taskWithAssets = updateAssetTaskAssets(task);
			assetImportChecked.add(taskWithAssets);
		};
		final AssetPathConflictChecker ops =
				assetPathConflictCheckerFactory.create(onAssetPathChecked);
		
		final Runnable onFinished = () -> {
			assetTaskLoader = null;
			isAssetImportCheckDone = true;
		};
		assetTaskLoader = new BundledMessageResponses<String>(pathToTask.keySet(), ops, onFinished);
		
		return assetTaskLoader.firstBundle();
	}
	
	public List<MessageResponse> firstTaskIdCheckBundle() {
		if(!isAssetImportCheckDone) {
			throw new IllegalStateException(
					"This method cannot be called until assetPath conflict checking has been completed!");
		}
		final java.util.Collection<Task> tasks = multiMap.get(GeneralTask.class);
		tasks.addAll(assetImportChecked);
		
		final TaskIdConflictChecker ops = taskIdConflictCheckerFactory.create();
		generalTaskLoader = new BundledMessageResponses<>(
				tasks, ops, ()->{generalTaskLoader = null;});
		
		return generalTaskLoader.firstBundle();
	}
	
	private <A extends Asset> AssetTask<A> updateAssetTaskAssets(AssetTask<A> task) {
		final AssetTaskService<A> service = serviceFinder.getAssetService(Util.classOf(task));
		service.updateAssetUpdatePreview(task, AssetGroupType.ORIGINAL);
		service.updateAssetUpdatePreview(task, AssetGroupType.NEW);
		return task;
	}
	
	public List<MessageResponse> nextCheckBundle(ConflictAnswer answer) {
		BundledMessageResponses<?> current;
		if (isAssetImportCheckDone) {
			current = generalTaskLoader;
		} else {
			current = assetTaskLoader;
		}
		Objects.requireNonNull(current);
		return current.nextBundle(answer);
	}
}
