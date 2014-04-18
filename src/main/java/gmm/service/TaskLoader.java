package gmm.service;

import gmm.domain.Task;
import gmm.domain.UniqueObject;
import gmm.service.data.DataAccess;
import gmm.service.data.XMLService;
import gmm.util.Collection;

import java.util.Iterator;

public class TaskLoader {
	
	DataAccess data = Spring.get(DataAccess.class);
	XMLService xmlService = Spring.get(XMLService.class);
	
	private Iterator<? extends Task> taskLoader;
	private Task currentlyLoaded;
	private String doForAll = "default";
	
	private static String defaultOp = "default";
	private static String skipOp = "skip";
	private static String overwriteOp = "overwrite";
	private static String bothOp = "both";
	
	@SuppressWarnings("unchecked")
	public TaskLoader(String path) {
		taskLoader = ((Collection<? extends Task>) xmlService.deserialize(path)).iterator();
	}
	
	
	public TaskLoaderResult loadNext(String operation, boolean doForAllFlag) {
		validate(operation);
		TaskLoaderResult result = new TaskLoaderResult();
		boolean needOperation;
		
		//If the user wants to process a new element, we try to do so.
		if(operation.equals(defaultOp)) {
			if (!taskLoader.hasNext()) {
				//Loading finished, user will stop sending requests!
				result.status = "finished";
				return result;
			}
			//We try to add the next element. If a conflict occurs, we may need to ask the user.
			currentlyLoaded = taskLoader.next();
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
			result.status = "success";
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
			result.status = "conflict";
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
	}
}
