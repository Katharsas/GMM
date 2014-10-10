package gmm.service;

import gmm.collections.LinkedList;
import gmm.collections.List;
import gmm.domain.Task;
import gmm.domain.UniqueObject;
import gmm.service.data.DataAccess;
import gmm.service.data.XMLService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;

/**
 * Provides a way to communicate with the client when the server needs to send a lot of
 * messages to the client and the client needs to be able to respond to any of those messages.
 * 
 * To not waste requests, the messages from the server will come in bundles of dynamic size, where
 * the last bundle message either indicates that the server needs a message from the client or that
 * the server has finished sending all messages.
 * 
 * Counterpart to js class "ResponseBundleHandler"
 */
public class TaskLoader {
	
	private final DataAccess data = Spring.get(DataAccess.class);
	
	private final Iterator<? extends Task> taskLoader;
	private Task currentlyLoaded;
	private String doForAll = "default";
	
	private static final String defaultOp = "default";
	private static final String skipOp = "skip";
	private static final String overwriteOp = "overwrite";
	private static final String bothOp = "both";
	
	private static final String success = "success";
	private static final String conflict = "conflict";
	private static final String finished = "finished";
	
	public TaskLoader(Path path) throws IOException {
		taskLoader = Spring.get(XMLService.class).deserialize(path, Task.class).iterator();
	}
	
	public List<TaskLoaderResult> loadNextBundle(String operation, boolean doForAllFlag) {
		List<TaskLoaderResult> results = new LinkedList<>();
		TaskLoaderResult result = loadNext(operation, doForAllFlag);
		boolean loadNext = result.status.equals(success);
		results.add(result);
		while(loadNext) {
			result = loadNext(defaultOp, false);
			results.add(result);
			loadNext = result.status.equals(success);
		}
		return results;
	}
	
	public TaskLoaderResult loadNext(String operation, boolean doForAllFlag) {
		validate(operation);
		TaskLoaderResult result = new TaskLoaderResult();
		boolean needOperation;
		
		//If the user wants to process a new element, we try to do so.
		if(operation.equals(defaultOp)) {
			if (!taskLoader.hasNext()) {
				//Loading finished, user will stop sending requests!
				result.status = finished;
				return result;
			}
			//We try to add the next element. If a conflict occurs, we may need to ask the user.
			currentlyLoaded = taskLoader.next();
			currentlyLoaded.onLoad();
			UniqueObject.updateCounter(currentlyLoaded);
			needOperation = !data.add(currentlyLoaded);
			//we only need a new operation for a conflict, if the user never answered "doForAll".
			if(needOperation && !doForAll.equals(defaultOp)) {
				operation = doForAll;
				needOperation = false;
			}
		}
		//Else, he gave an answer on how to handle the last (conflicting) element (and maybe "doForAll").
		else {
			needOperation = false;
			if(doForAllFlag) {
				doForAll = operation;
			}
		}
		
		String name = currentlyLoaded.getName();
		String id = ""+currentlyLoaded.getId();
		
		//If we have an operation now, we use it.
		if(!needOperation) {
			result.status = success;
			if(operation.equals(defaultOp)) {
				result.message = "Successfully added task [id: "+id+", name: "+name+"]";
			}
			else if(operation.equals(skipOp)) {
				result.message = "Skipping conflicting task [id: "+id+", name: "+name+"]";
			}
			else if(operation.equals(overwriteOp)) {
				result.message = "Overwriting task with conflicting task [id: "+id+", name: "+name+"]";
				data.remove(currentlyLoaded);
				data.add(currentlyLoaded);
			}
			
			else if(operation.equals(bothOp)) {
				currentlyLoaded.makeUnique();
				result.message = "Adding conflicting task as new task [id: "+id+", name: "+name+"]";
				data.add(currentlyLoaded);
			}
		}
		//If not, we need to get an operation from the user. The user will then give us an answer.
		else{
			result.status = conflict;
			result.message = "Conflict: A task with id: "+id+" already exists!";
		}
		return result;
	}
	
	private void validate(String o) {
		if (!(o.equals(defaultOp) || o.equals(skipOp) || o.equals(overwriteOp) || o.equals(bothOp))) {
			throw new IllegalArgumentException("This operation is not allowed! ");
		}
	}

	public static class TaskLoaderResult {
		public String status;
		public String message;
		@Override
		public String toString() {
			return "Status: "+status+", "+"Message: "+message;
		}
	}
}
