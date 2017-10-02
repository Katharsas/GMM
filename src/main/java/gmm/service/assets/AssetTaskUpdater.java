package gmm.service.assets;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Service;

import gmm.domain.task.asset.AssetGroupType;
import gmm.domain.task.asset.AssetProperties;
import gmm.domain.task.asset.AssetTask;
import gmm.service.assets.NewAssetFolderInfo.AssetFolderStatus;
import gmm.service.tasks.AssetTaskService;
import gmm.service.tasks.TaskServiceFinder;
import gmm.util.Util;

/**
 * This service implements the state machines that model how AssetTask properties & info need to change when a task or
 * its assets info changes. Allows to define "preconditions", which express one of the states an AssetTask must be in
 * to be allowed to transform to the new state.
 * 
 * @author Jan Mothes
 */
@Service
public class AssetTaskUpdater {
	
	public static enum Properties {
		NULL, EXISTS
	}
	
	public static enum Asset {
		NULL, NO_ASSET, VALID_ASSET
	}
	
	public static class TaskStateCondition {
		
		public final AssetTask<?> actual;
		public final Properties expectedProperties;
		public final Asset expectedAssetFolder;
		
		public TaskStateCondition(AssetTask<?> actual, Properties expectedProperties, Asset expectedAssetFolder) {
			this.actual = actual;
			this.expectedProperties = expectedProperties;
			this.expectedAssetFolder = expectedAssetFolder;
		}
		public boolean check(AssetGroupType type) {
			final Properties actualProps = actual.getNewAssetProperties() == null ?
					Properties.NULL : Properties.EXISTS;
			final Asset actualFolder = getState(type, Optional.ofNullable(actual.getNewAssetFolderInfo()));
			return actualProps == expectedProperties && actualFolder == expectedAssetFolder;
		}
		public static Asset getState(AssetGroupType type, Optional<AssetInfo> info) {
			if (!info.isPresent()) return Asset.NULL;
			else {
				if (type.isOriginal()) return Asset.VALID_ASSET;
				else {
					final NewAssetFolderInfo folderInfo = (NewAssetFolderInfo) info.get();
					return folderInfo.getStatus() == AssetFolderStatus.VALID_WITH_ASSET ?
							Asset.VALID_ASSET : Asset.NO_ASSET;
				}
			}
		}
		public static void checkAny(AssetGroupType type, TaskStateCondition... allowed) {
			for (final TaskStateCondition condition : allowed) {
				if (condition.check(type)) return;
			}
			throw new IllegalStateException("Actual task state does not match any given states!");
		}
	}
	
	private final TaskServiceFinder serviceFinder;
	
	private final Map<AssetTask<?>, CompletableFuture<Void>> processingAssetTasks;
	
	public AssetTaskUpdater(TaskServiceFinder serviceFinder) {
		this.serviceFinder = serviceFinder;
		
		processingAssetTasks = new HashMap<>();
	}
	
	private <A extends AssetProperties> AssetTaskService<A> getService(AssetTask<A> task) {
		return serviceFinder.getAssetService(Util.classOf(task));
	}
	
	/**
	 * Create asset task properties asynchronously and save the future to a map so we can later check if we need to
	 * wait for that future to finish. A finished future will delete itself from the list.
	 */
	private synchronized <A extends AssetProperties> void recreateAssetPropertiesAndInfo(
			AssetTask<A> task, AssetInfo info, Optional<Runnable> onCompletion) {
		
		CompletableFuture<Void> future = getService(task).recreateAssetProperties(task, info);
		if (onCompletion.isPresent()) {
			future = future.thenRun(onCompletion.get());
		}
		if (!future.isDone()) {
			processingAssetTasks.put(task, future.thenRun(
					() -> processingAssetTaskFinished(task)));
		}
	}
	
	private synchronized void processingAssetTaskFinished(AssetTask<?> task) {
		processingAssetTasks.remove(task);
	}
	
	private <A extends AssetProperties> void removeNewAssetPropertiesAndSetInfo(AssetTask<A> task, Optional<NewAssetFolderInfo> info) {
		getService(task).removeNewAssetProperties(task, info);
	}
	
	private <A extends AssetProperties> void removeOriginalAssetProperties(AssetTask<A> task) {
		getService(task).removeOriginalAssetProperties(task);
	}
	
	private <A extends AssetProperties> void changeNewAssetInfo(AssetTask<A> task, Optional<NewAssetFolderInfo> info) {
		getService(task).changeNewAssetInfo(task, info);
	}
	
	private <A extends AssetProperties> void changeOriginalAssetInfo(AssetTask<A> task, OriginalAssetFileInfo info) {
		getService(task).changeOriginalAssetInfo(task, info);
	}
	
	/**
	 * @see {@link #waitForAsyncTaskProcessing(AssetTask)}
	 */
	public void waitForAllAsyncTaskProcessings() {
		for (final AssetTask<?> task : processingAssetTasks.keySet()) {
			waitForAsyncTaskProcessing(task);
		}
	}
	
	public synchronized CompletableFuture<Void> allAyncTaskProcessing() {
		final Collection<CompletableFuture<Void>> futures = processingAssetTasks.values();
		return CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
	}
	
	/**
	 * Since asset task properties & info can be changed asynchronously, check that for the given task nothing is still
	 * running (or block until it is finished).
	 */
	public synchronized void waitForAsyncTaskProcessing(AssetTask<?> task) {
		final CompletableFuture<Void> old = processingAssetTasks.get(task);
		if (old != null) old.join();
	}
	
	public class AsyncPreviewCreationException extends RuntimeException {
		private static final long serialVersionUID = 6877212615078244866L;
		public AsyncPreviewCreationException(String message, ExecutionException e) {
			super(message, e.getCause());
		}
	}
	
	public abstract class OnUpdate {
		
		private boolean usedUp = false;
		protected final Optional<Runnable> onCompletion;
		
		public OnUpdate() {
			this.onCompletion = Optional.empty();
		}
		public OnUpdate(Runnable onCompletion) {
			this.onCompletion = Optional.of(onCompletion);
		}
		
		protected synchronized void doUpdate(Runnable updateMethod) {
			if (usedUp) throw new IllegalStateException("Updater can only used for one update operation!");
			updateMethod.run();
			usedUp = true;
		}
		
		protected void onCompletion() {
			if (onCompletion.isPresent()) onCompletion.get().run();
		}
	}
	
	public class OnNewAssetUpdate extends OnUpdate {
		
		AssetGroupType type = AssetGroupType.NEW;
		
		public OnNewAssetUpdate() {
			super();
		}
		public OnNewAssetUpdate(Runnable onCompletion) {
			super(onCompletion);
		}
		
		/**
		 * @param validAssetInfo - status of info must be {@link AssetFolderStatus#VALID_WITH_ASSET}
		 */
		public <A extends AssetProperties> void recreatePropsAndSetInfo(AssetTask<A> task, NewAssetFolderInfo validAssetInfo) {
			doUpdate(() -> {
				TaskStateCondition.checkAny(type,
					new TaskStateCondition(task, Properties.NULL, Asset.NO_ASSET),
					new TaskStateCondition(task, Properties.NULL, Asset.NULL),
					new TaskStateCondition(task, Properties.EXISTS, Asset.VALID_ASSET)
				);
				recreateAssetPropertiesAndInfo(task, validAssetInfo, onCompletion);
			});
		}
		
		/**
		 * @param noAssetOrNullInfo - if not empty, status of info must NOT be {@link AssetFolderStatus#VALID_WITH_ASSET}
		 */
		public void removePropsAndSetInfo(AssetTask<?> task, Optional<NewAssetFolderInfo> noAssetOrNullInfo) {
			doUpdate(() -> {
				TaskStateCondition.checkAny(type,
					new TaskStateCondition(task, Properties.EXISTS, Asset.VALID_ASSET)
				);
				removeNewAssetPropertiesAndSetInfo(task, noAssetOrNullInfo);
				onCompletion();
			});
		}
		
		/**
		 * @param anyInfo - if equals {@link AssetFolderStatus#VALID_WITH_ASSET}, the corresponding properties must
		 * exist currently on that task, otherwise properties must currently be null.
		 */
		public void setInfo(AssetTask<?> task, Optional<NewAssetFolderInfo> anyInfo) {
			doUpdate(() -> {
				TaskStateCondition.checkAny(type,
					new TaskStateCondition(task, Properties.NULL, Asset.NO_ASSET),
					new TaskStateCondition(task, Properties.NULL, Asset.NULL),
					new TaskStateCondition(task, Properties.EXISTS, Asset.VALID_ASSET)
				);
				changeNewAssetInfo(task, anyInfo);
				onCompletion();
			});
		}
	}
	
	
	public class OnOriginalAssetUpdate extends OnUpdate {
		
		AssetGroupType type = AssetGroupType.ORIGINAL;
		
		public OnOriginalAssetUpdate() {
			super();
		}
		public OnOriginalAssetUpdate(Runnable onCompletion) {
			super(onCompletion);
		}
		
		public void recreatePropsAndSetInfo(AssetTask<?> task, OriginalAssetFileInfo info) {
			doUpdate(() -> {
				TaskStateCondition.checkAny(type,
					new TaskStateCondition(task, Properties.NULL, Asset.NULL),
					new TaskStateCondition(task, Properties.EXISTS, Asset.VALID_ASSET)
				);
				recreateAssetPropertiesAndInfo(task, info, onCompletion);
			});
		}
		
		public void removePropsAndInfo(AssetTask<?> task) {
			doUpdate(() -> {
				TaskStateCondition.checkAny(type,
					new TaskStateCondition(task, Properties.EXISTS, Asset.VALID_ASSET)
				);
				removeOriginalAssetProperties(task);
				onCompletion();
			});
		}
		
		public void setInfo(AssetTask<?> task, OriginalAssetFileInfo info) {
			doUpdate(() -> {
				TaskStateCondition.checkAny(type,
					new TaskStateCondition(task, Properties.EXISTS, Asset.VALID_ASSET)
				);
				changeOriginalAssetInfo(task, info);
				onCompletion();
			});
		}
	}
}
