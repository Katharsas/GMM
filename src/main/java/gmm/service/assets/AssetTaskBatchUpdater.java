package gmm.service.assets;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import gmm.collections.ArrayList;
import gmm.collections.Collection;
import gmm.domain.task.asset.AssetTask;
import gmm.service.assets.AssetTaskUpdater.OnNewAssetUpdate;
import gmm.service.assets.AssetTaskUpdater.OnOriginalAssetUpdate;

@Service
public class AssetTaskBatchUpdater {
	
	public static interface OnBatchComplete extends Consumer<Collection<AssetTask<?>>> {}
	public static interface OnAssetUpdateProvider {
		public OnOriginalAssetUpdate createOnOriginalAssetUpdate();
		public OnNewAssetUpdate createOnNewAssetUpdate();
	}
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private final AssetTaskUpdater taskUpdater;
	private final Set<ResultCollector> remaining = ConcurrentHashMap.newKeySet();
	
	
	public class UpdateProvider implements OnAssetUpdateProvider {
		private final Consumer<AssetTask<?>> onComplete;
		private OnOriginalAssetUpdate originalUpdate = null;
		private OnNewAssetUpdate newUpdate = null;
		public UpdateProvider(Consumer<AssetTask<?>> onComplete) {
			this.onComplete = onComplete;
		}
		@Override
		public OnOriginalAssetUpdate createOnOriginalAssetUpdate() {
			if (originalUpdate == null) {
				originalUpdate = taskUpdater.new OnOriginalAssetUpdate(onComplete);
			}
			return originalUpdate;
		}
		@Override
		public OnNewAssetUpdate createOnNewAssetUpdate() {
			if (newUpdate == null) {
				newUpdate = taskUpdater.new OnNewAssetUpdate(onComplete);
			}
			return newUpdate;
		}
		private int calledUpdateOperations() {
			int count = 0;
			if (originalUpdate != null) {
				if (originalUpdate.isUsed()) count++;
			}
			if (newUpdate != null) {
				if (newUpdate.isUsed()) count++;
			}
			return count;
		}
	}
	
	public class ResultCollector {
		
		// TODO should probably use AssetTask ids instead of the tasks themselves (for immutable tasks)
		private final ConcurrentHashMap<AssetTask<?>, AtomicInteger> pendingAsyncCompletions = new ConcurrentHashMap<>();
		private final OnBatchComplete onComplete;
		
		public ResultCollector(OnBatchComplete onComplete) {
			this.onComplete = onComplete;
		}
		
		public void updateTask(AssetTask<?> toUpdate, BiConsumer<AssetTask<?>, OnAssetUpdateProvider> updateFunction) {
			UpdateProvider updateProvider = new UpdateProvider(completedTask -> {
				AtomicInteger pendingUpdates = pendingAsyncCompletions.get(completedTask);
				if (pendingUpdates == null) {
					logger.error("Could not retrieve AssetTask pending update counter!");
				} else {
					pendingUpdates.getAndDecrement();
				}
			});
			AtomicInteger updateCount = new AtomicInteger(0);
			pendingAsyncCompletions.put(toUpdate, updateCount);
			updateFunction.accept(toUpdate, updateProvider);
			updateCount.set(updateCount.get() + updateProvider.calledUpdateOperations());
		}
		
		/**
		 * @return true if all tasks have been completed and no more batches are left, false otherwise.
		 */
		private boolean completeBatch() {
			if (!pendingAsyncCompletions.isEmpty()) {
				Collection<AssetTask<?>> resultBatch = new ArrayList<>(AssetTask.getGenericClass());
				pendingAsyncCompletions.entrySet().removeIf(entry -> {
					if (entry.getValue().get() <= 0) {
						resultBatch.add(entry.getKey());
						return true;
					} else {
						return false;
					}
				});
				if (resultBatch.size() > 0) {
					onComplete.accept(resultBatch);
				}
			}
			return pendingAsyncCompletions.isEmpty();
		}
	}
	
	
	@Autowired
	public AssetTaskBatchUpdater(AssetTaskUpdater taskUpdater) {
		this.taskUpdater = taskUpdater;
	}

	public void updateTasks(
			Collection<? extends AssetTask<?>> toUpdate,
			BiConsumer<AssetTask<?>, OnAssetUpdateProvider> updateFunction,
			OnBatchComplete onComplete) {
		
		final ResultCollector resultCollector = new ResultCollector(onComplete);
		for (final AssetTask<?> task : toUpdate) {
			resultCollector.updateTask(task, updateFunction);
		}
		final boolean haveAllCompleted = resultCollector.completeBatch();
		if (!haveAllCompleted) {
			remaining.add(resultCollector);
		}
	}
	
	@Scheduled(fixedDelay = 5000)
	private void callback() {
		if (remaining.size() > 0) {
			for (final Iterator<ResultCollector> i = remaining.iterator(); i.hasNext();) {
				final ResultCollector resultCollector = i.next();
				final boolean haveAllCompleted = resultCollector.completeBatch();
				if (haveAllCompleted) {
					i.remove();
				}
			}
			if (remaining.size() > 20) {
				logger.warn("High number of uncompleted batch processes. Leak?");
			}
		}
	}
}
