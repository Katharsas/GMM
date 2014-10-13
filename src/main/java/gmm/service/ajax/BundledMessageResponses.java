package gmm.service.ajax;

import gmm.collections.LinkedList;
import gmm.collections.List;
import gmm.domain.UniqueObject;

import java.io.IOException;
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
 * 
 * @author Jan Mothes
 * @param <T> Type of elements that are being operated on
 */
public class BundledMessageResponses<T extends UniqueObject> {
	
	private final MessageResponseOperations<T> ops;
	
	private final Iterator<T> elements;
	private T currentlyLoaded;
	private String doForAll = "default";
	
	private static final String defaultOp = "default";
	
	private static final String success = "success";
	private static final String conflict = "conflict";
	private static final String finished = "finished";
	
	public BundledMessageResponses(Iterator<T> elements,
			MessageResponseOperations<T> ops) throws IOException {
		this.elements = elements;
		this.ops = ops;
	}
	
	public List<MessageResponse> loadNextBundle(String operation, boolean doForAllFlag) {
		List<MessageResponse> results = new LinkedList<>();
		MessageResponse result = loadNext(operation, doForAllFlag);
		boolean loadNext = result.status.equals(success);
		results.add(result);
		while(loadNext) {
			result = loadNext(defaultOp, false);
			results.add(result);
			loadNext = result.status.equals(success);
		}
		return results;
	}
	
	private MessageResponse loadNext(String operation, boolean doForAllFlag) {
		MessageResponse result = new MessageResponse();
		boolean needOperation;
		
		//If the user wants to process a new element, we try to do so.
		if(operation.equals(defaultOp)) {
			if (!elements.hasNext()) {
				//Loading finished, user will stop sending requests!
				result.status = finished;
				return result;
			}
			//We try to add the next element. If a conflict occurs, we may need to ask the user.
			currentlyLoaded = elements.next();
			needOperation = ops.onLoad(currentlyLoaded);
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
		
		//If we have an operation now, we use it.
		if(!needOperation) {
			result.status = success;
			if(operation.equals(defaultOp)) {
				result.message = ops.onDefault(currentlyLoaded);}
			else {
				result.message = ops.doOperation(operation, currentlyLoaded);}
		}
		//If not, we need to get an operation from the user. The user will then give us an answer.
		else{
			result.status = conflict;
			result.message = ops.onConflict(currentlyLoaded);
		}
		return result;
	}
}
