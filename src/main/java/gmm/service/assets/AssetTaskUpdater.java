package gmm.service.assets;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import gmm.domain.task.asset.AssetGroupType;
import gmm.domain.task.asset.AssetProperties;
import gmm.domain.task.asset.AssetTask;
import gmm.service.assets.NewAssetFolderInfo.AssetFolderStatus;
import gmm.service.tasks.AssetTaskService;
import gmm.service.tasks.TaskServiceFinder;
import gmm.util.Util;

/**
 * This service implements the state machines that model how AssetTask properties & info need to
 * change when a task or its assets info changes. Allows to define "preconditions", which express
 * one of the states an AssetTask must be in to be allowed to transform to the new state.
 * 
 * To the caller, two inner classes are provided for original and new asset updates respectively.
 * 
 * @author Jan Mothes
 */
@Service
public class AssetTaskUpdater {
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	public static enum Properties {
		NULL, EXISTS
	}
	
	public static enum Asset {
		NULL, NO_ASSET, VALID_ASSET
	}
	
	public static class TaskStateCondition {
		
		public final Properties expectedProperties;
		public final Asset expectedInfo;
		
		public TaskStateCondition(Properties expectedProperties, Asset expectedInfo) {
			this.expectedProperties = expectedProperties;
			this.expectedInfo = expectedInfo;
		}
		public boolean check(AssetGroupType type, AssetTask<?> actual) {
			final Properties actualProps = actual.getAssetProperties(type) == null ?
					Properties.NULL : Properties.EXISTS;
			final Asset actualInfo = getState(type, Optional.ofNullable(actual.getAssetStorageInfo(type)));
			return actualProps == expectedProperties && actualInfo == expectedInfo;
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
		public static void checkAny(AssetGroupType type, AssetTask<?> actual, TaskStateCondition... allowed) {
			for (final TaskStateCondition condition : allowed) {
				if (condition.check(type, actual)) return;
			}
			throw new IllegalStateException("Actual task state does not match any given states! Task: '" + actual +"'");
		}
	}
	
	private final TaskServiceFinder serviceFinder;
	private final NewAssetLockService lockService;
	
	private final Map<AssetTask<?>, CompletableFuture<Void>> processingAssetTasks;
	
	public AssetTaskUpdater(TaskServiceFinder serviceFinder, NewAssetLockService lockService) {
		this.serviceFinder = serviceFinder;
		this.lockService = lockService;
		
		processingAssetTasks = new HashMap<>();
	}
	
	private <A extends AssetProperties> AssetTaskService<A> getService(AssetTask<A> task) {
		return serviceFinder.getAssetService(Util.classOf(task));
	}
	
	/**
	 * Create asset task properties asynchronously and save the future to a map so we can later
	 * check if we need to wait for that future to finish. A finished future will delete itself
	 * from the list.
	 */
	private synchronized <A extends AssetProperties> void recreateAssetPropertiesAndInfo(
			AssetTask<A> task, AssetInfo info, Optional<Consumer<AssetTask<?>>> onCompletion) {
		
		lockService.fixReadLock("AssetTaskUpdater::recreateAssetPropertiesAndInfo");
		CompletableFuture<AssetTask<?>> future = getService(task).recreateAssetProperties(task, info);
		future = future.exceptionally(e -> {
			logger.error("Asset task preview creation threw exception.", e);
			return task;
		});
		CompletableFuture<Void> complete;
		if (onCompletion.isPresent()) {
			complete = future.thenAccept(onCompletion.get());
		} else {
			complete = future.thenAccept(result -> {});
		}
		complete = complete.exceptionally(e -> {
			logger.error("Asset task preview completion callback threw exception.", e);
			return null;
		});
		if (!future.isDone()) {
			processingAssetTasks.put(task, complete);
			complete = complete.thenRun(() -> {
				synchronized(this) {
					processingAssetTasks.remove(task);
				}
			}).exceptionally(e -> {
				logger.error(null, e);
				return null;
			});
		}
	}
	
	private <A extends AssetProperties> AssetTask<A> removeNewAssetPropertiesAndSetInfo(
			AssetTask<A> task, Optional<NewAssetFolderInfo> info) {
		return getService(task).removeNewAssetProperties(task, info);
	}
	
	private <A extends AssetProperties> AssetTask<A> removeOriginalAssetProperties(
			AssetTask<A> task) {
		return getService(task).removeOriginalAssetProperties(task);
	}
	
	private <A extends AssetProperties> AssetTask<A> changeNewAssetInfo(
			AssetTask<A> task, Optional<NewAssetFolderInfo> info) {
		return getService(task).changeNewAssetInfo(task, info);
	}
	
	private <A extends AssetProperties> AssetTask<A> changeOriginalAssetInfo(
			AssetTask<A> task, OriginalAssetFileInfo info) {
		return getService(task).changeOriginalAssetInfo(task, info);
	}
	
	public synchronized CompletableFuture<Void> allAyncTaskProcessing() {
		final Collection<CompletableFuture<Void>> futures = processingAssetTasks.values();
		return CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
	}
	
	private boolean isAssetImportRunning() {
		return processingAssetTasks.size() > 0;
	}
	
	@Scheduled(fixedRate=1000)
	private synchronized void attemptOpenNewAssetLock() {
		if (!isAssetImportRunning()) {
			lockService.attemptUnfixReadLock("AssetTaskUpdater::attemptOpenNewAssetLock");
		}
	}
	
	public class AsyncPreviewCreationException extends RuntimeException {
		private static final long serialVersionUID = 6877212615078244866L;
		public AsyncPreviewCreationException(String message, ExecutionException e) {
			super(message, e.getCause());
		}
	}
	
	public abstract class OnUpdate {
		
		private boolean usedUp = false;
		protected final Optional<Consumer<AssetTask<?>>> onCompletion;
		
		public OnUpdate(Optional<Consumer<AssetTask<?>>> onCompletion) {
			this.onCompletion = onCompletion;
		}
		
		protected synchronized void doUpdate(Runnable updateMethod) {
			if (usedUp) throw new IllegalStateException("Updater can only used for one update operation!");
			updateMethod.run();
			usedUp = true;
		}
		
		protected synchronized void doUpdateAndComplete(Supplier<AssetTask<?>> updateMethod) {
			if (usedUp) throw new IllegalStateException("Updater can only used for one update operation!");
			final AssetTask<?> result = updateMethod.get();
			usedUp = true;
			if (onCompletion.isPresent()) onCompletion.get().accept(result);
		}
	}
	
	public class OnNewAssetUpdate extends OnUpdate {
		
		AssetGroupType type = AssetGroupType.NEW;
		
		/**
		 * Create an update to execute one of the available operations, some of which are async.
		 */
		public OnNewAssetUpdate() {
			this(null);
		}
		
		/**
		 * Create an update to execute one of the available operations, some of which are async.
		 * After an operation has been completed, the given Runnable will be executed (can be used to
		 * call data.edit(task) to broadcast task change for example).
		 */
		public OnNewAssetUpdate(Consumer<AssetTask<?>> onCompletion) {
			super(Optional.ofNullable(onCompletion));
		}
		
		/**
		 * Async.
		 * The calling thread must own the lock available from {@link NewAssetLockService}.
		 * @param validAssetInfo - status of info must be {@link AssetFolderStatus#VALID_WITH_ASSET}
		 */
		public <A extends AssetProperties> void recreatePropsAndSetInfo(AssetTask<A> task, NewAssetFolderInfo validAssetInfo) {
			doUpdate(() -> {
				TaskStateCondition.checkAny(type, task,
					new TaskStateCondition(Properties.NULL, Asset.NO_ASSET),
					new TaskStateCondition(Properties.NULL, Asset.NULL),
					new TaskStateCondition(Properties.EXISTS, Asset.VALID_ASSET)
				);
				recreateAssetPropertiesAndInfo(task, validAssetInfo, onCompletion);
			});
		}
		
		/**
		 * @param noAssetOrNullInfo - if not empty, status of info must NOT be {@link AssetFolderStatus#VALID_WITH_ASSET}
		 */
		public void removePropsAndSetInfo(AssetTask<?> task, Optional<NewAssetFolderInfo> noAssetOrNullInfo) {
			doUpdateAndComplete(() -> {
				TaskStateCondition.checkAny(type, task,
					new TaskStateCondition(Properties.EXISTS, Asset.VALID_ASSET)
				);
				return removeNewAssetPropertiesAndSetInfo(task, noAssetOrNullInfo);
			});
		}
		
		/**
		 * @param anyInfo - if equals {@link AssetFolderStatus#VALID_WITH_ASSET}, the corresponding properties must
		 * exist currently on that task, otherwise properties must currently be null.
		 */
		public void setInfo(AssetTask<?> task, Optional<NewAssetFolderInfo> anyInfo) {
			doUpdateAndComplete(() -> {
				TaskStateCondition.checkAny(type, task,
					new TaskStateCondition(Properties.NULL, Asset.NO_ASSET),
					new TaskStateCondition(Properties.NULL, Asset.NULL),
					new TaskStateCondition(Properties.EXISTS, Asset.VALID_ASSET)
				);
				return changeNewAssetInfo(task, anyInfo);
			});
		}
	}
	
	
	public class OnOriginalAssetUpdate extends OnUpdate {
		
		AssetGroupType type = AssetGroupType.ORIGINAL;
		
		/**
		 * Create an update to execute one of the available operations asynchronously.
		 */
		public OnOriginalAssetUpdate() {
			this(null);
		}

		/**
		 * Create an update to execute one of the available operations asynchronously.
		 * After the async operation has been completed, the given Runnable will be executed (can be
		 * used to call data.edit(task) to broadcast task change for example).
		 */
		public OnOriginalAssetUpdate(Consumer<AssetTask<?>> onCompletion) {
			super(Optional.ofNullable(onCompletion));
		}
		
		public void recreatePropsAndSetInfo(AssetTask<?> task, OriginalAssetFileInfo info) {
			doUpdate(() -> {
				TaskStateCondition.checkAny(type, task,
					new TaskStateCondition(Properties.NULL, Asset.NULL),
					new TaskStateCondition(Properties.EXISTS, Asset.VALID_ASSET)
				);
				recreateAssetPropertiesAndInfo(task, info, onCompletion);
			});
		}
		
		public void removePropsAndInfo(AssetTask<?> task) {
			doUpdateAndComplete(() -> {
				TaskStateCondition.checkAny(type, task,
					new TaskStateCondition(Properties.EXISTS, Asset.VALID_ASSET)
				);
				return removeOriginalAssetProperties(task);
			});
		}
		
		public void setInfo(AssetTask<?> task, OriginalAssetFileInfo info) {
			doUpdateAndComplete(() -> {
				TaskStateCondition.checkAny(type, task,
					new TaskStateCondition(Properties.EXISTS, Asset.VALID_ASSET)
				);
				return changeOriginalAssetInfo(task, info);
			});
		}
	}
}
