package gmm.service.ajax;


import gmm.collections.LinkedList;
import gmm.collections.List;
import gmm.domain.task.Task;
import gmm.domain.task.asset.Asset;
import gmm.domain.task.asset.AssetTask;
import gmm.service.ajax.operations.AssetImportOperations;
import gmm.util.Util;
import gmm.util.Util.SingleIterator;
import gmm.web.forms.TaskForm;

/**
 * Simple wrapper for {@link BundledMessageResponses} that should be used when checking
 * a single new user-created Task for conflicts with {@link AssetImportOperations}.
 * 
 * @author Jan Mothes
 */
public class NewTaskResponses {
	
	private BundledMessageResponses<String> importer;
	private boolean isAsset;
	
	public NewTaskResponses(Task task, TaskForm form) {
		if(task instanceof AssetTask) {
			isAsset = true;
			AssetTask<?> assetTask = (AssetTask<?>) task;
			AssetImportOperations<? extends Asset, ? extends AssetTask<?>> ops =
					new AssetImportOperations<>(form, Util.getClass(assetTask));
			SingleIterator<String> it =
					new SingleIterator<>(assetTask.getAssetPath().toString());
			
			this.importer = new BundledMessageResponses<>(it, ops);
		} else {
			// if not an asset, loadFirst will return success (no checks needed)
			isAsset = false;
		}
	}
	
	public List<MessageResponse> loadFirst() {
		if(isAsset) {
			return importer.loadFirstBundle();
		} else {
			String message = "Successfully added new task!";
			MessageResponse finished =
					new MessageResponse(BundledMessageResponses.finished, message);
			LinkedList<MessageResponse> result = new LinkedList<>(MessageResponse.class);
			result.add(finished);
			return result;
		}
	}
	
	public List<MessageResponse> loadNext(String operation) {
		if(isAsset) {
			return importer.loadNextBundle(operation, false);
		} else {
			// TODO create special exception for violations of BundledMessageResponses protocol.
			throw new IllegalStateException(
					"Method 'loadFirst' must have returned 'finished' status."
					+ " Client ist not allowed to send answer on 'finish'!");
		}
	}
}
