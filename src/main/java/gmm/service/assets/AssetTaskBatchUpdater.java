package gmm.service.assets;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import gmm.collections.Collection;
import gmm.collections.HashSet;
import gmm.domain.task.asset.AssetTask;
import gmm.service.assets.AssetTaskUpdater.OnNewAssetUpdate;
import gmm.service.assets.AssetTaskUpdater.OnOriginalAssetUpdate;

@Service
public class AssetTaskBatchUpdater {
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	public static interface OnAssetUpdateProvider {
		public OnOriginalAssetUpdate createOnOriginalAssetUpdate();
		public OnNewAssetUpdate createOnNewAssetUpdate();
	}
	
	public static interface OnBatchComplete extends Consumer<Collection<AssetTask<?>>> {}
	
	public class ResultCollector implements OnAssetUpdateProvider {
		
		private final Collection<AssetTask<?>> updatedTasks = new HashSet<>(AssetTask.getGenericClass());
		private final OnBatchComplete onComplete;
		int remainingSize;
		
		public ResultCollector(int totalTaskNumber, OnBatchComplete onComplete) {
			this.onComplete = onComplete;
			remainingSize = totalTaskNumber;
		}
		
		@Override
		public OnOriginalAssetUpdate createOnOriginalAssetUpdate() {
			return taskUpdater.new OnOriginalAssetUpdate(result -> {
				synchronized (updatedTasks) {
					updatedTasks.add(result);
				}
			});
		}
		
		@Override
		public OnNewAssetUpdate createOnNewAssetUpdate() {
			return taskUpdater.new OnNewAssetUpdate(result -> {
				synchronized (updatedTasks) {
					updatedTasks.add(result);
				}
			});
		}
		
		/**
		 * @return true if all tasks have been completed an no more batches are left, false otherwise.
		 */
		private boolean completeBatch() {
			synchronized (updatedTasks) {
				if (updatedTasks.size() > 0) {
					Collection<AssetTask<?>> resultBatch;
					resultBatch = updatedTasks.copy();
					updatedTasks.clear();
					final int completedSize = resultBatch.size();
					onComplete.accept(resultBatch);
					remainingSize -= completedSize;
				}
			}
			return remainingSize <= 0;
		}
	}
	
	@Autowired private AssetTaskUpdater taskUpdater;
	
	private final Set<ResultCollector> remaining = ConcurrentHashMap.newKeySet();
	
	public void updateTasks(
			Collection<AssetTask<?>> toUpdate,
			BiConsumer<AssetTask<?>, OnAssetUpdateProvider> updateFunction,
			OnBatchComplete onComplete) {
		
		final ResultCollector resultCollector = new ResultCollector(toUpdate.size(), onComplete);
		for (final AssetTask<?> task : toUpdate ) {
			updateFunction.accept(task, resultCollector);
		}
		final boolean haveAllCompleted = resultCollector.completeBatch();
		if (!haveAllCompleted) {
			remaining.add(resultCollector);
		}
	}
	
	@Scheduled(fixedDelay = 10000)
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
				logger.warn("High number of uncompleted batches. Leak?");
			}
		}
	}
}
