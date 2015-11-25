package gmm.service.ajax;

import gmm.collections.LinkedList;
import gmm.collections.List;
import gmm.service.ajax.operations.MessageResponseOperations;
import gmm.service.ajax.operations.MessageResponseOperations.Conflict;
import gmm.service.ajax.operations.MessageResponseOperations.Operation;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Provides a way to communicate with the client when the server needs to send a lot of
 * messages to the client and the client needs to be able to respond to any of those messages.
 * 
 * To not waste requests, the messages from the server will come in bundles of dynamic size, where
 * the last bundle message either indicates that the server needs a message from the client or that
 * the server has finished sending all messages.
 * If the server needs more than two seconds while creating a bundle, it will send the responses
 * anyway. In this case, the last message does not indicate anything and the client can just request
 * the next bundle.
 * 
 * Counterpart to js class "ResponseBundleHandler"
 * 
 * @author Jan Mothes
 * @param <T> Type of elements that are being operated on
 */
public class BundledMessageResponses<T> {
	
	private final MessageResponseOperations<T> ops;
	private final Map<String, Operation<T>> operations;
	
	/**
	 * A conflict gets added if the user checks the "doForAll" button.
	 * Conflicts are mapped to the operation the user chose when he checked the button.
	 */
	private final Map<Conflict<T>, String> doForAlls = new HashMap<>();
	private Conflict<T> currentConflict = 
			MessageResponseOperations.cast(MessageResponseOperations.NO_CONFLICT);
	
	private final Iterator<? extends T> elements;
	private T currentlyLoaded;
	
	protected static final String defaultOp = "default";
	protected static final String success = "success";
	protected static final String finished = "finished";
	
	public BundledMessageResponses(Iterator<? extends T> elements, MessageResponseOperations<T> ops) throws IOException {
		this.elements = elements;
		this.ops = ops;
		operations = ops.getOperations();
	}
	
	public List<MessageResponse> loadFirstBundle() throws Exception {
		return loadNextBundle(defaultOp, false);
	}
	
	public List<MessageResponse> loadNextBundle(String operation, boolean doForAllFlag) throws Exception {
		List<MessageResponse> results = new LinkedList<>(MessageResponse.class);
		MessageResponse result = loadNext(operation, doForAllFlag);
		boolean loadNext = result.status.equals(success);
		results.add(result);
		long timeStamp = System.currentTimeMillis();
		while(loadNext) {
			result = loadNext(defaultOp, false);
			results.add(result);
			long duration = System.currentTimeMillis() - timeStamp;
			loadNext = result.status.equals(success) && duration < 2000;
		}
		return results;
	}
	
	private MessageResponse loadNext(String operation, boolean doForAllFlag) throws Exception {
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
			currentConflict = ops.onLoad(currentlyLoaded);
			needOperation = !currentConflict.equals(MessageResponseOperations.NO_CONFLICT);
			
			//we only need a new operation for a conflict, if the user never answered "doForAll".
			if(needOperation && doForAlls.containsKey(currentConflict)) {
				operation = doForAlls.get(currentConflict);
				needOperation = false;
			}
		}
		//Else, he gave an answer on how to handle the last (conflicting) element (and maybe "doForAll").
		else {
			needOperation = false;
			if(doForAllFlag) {
				doForAlls.put(currentConflict, operation);
			}
		}
		
		//If we have an operation now, we use it.
		if(!needOperation) {
			result.status = success;
			if(operation.equals(defaultOp)) {
				result.message = ops.onDefault(currentlyLoaded);}
			else {
				result.message = doOperation(operation, currentlyLoaded);}
		}
		//If not, we need to get an operation from the user. The user will then give us an answer.
		else{
			result.status = currentConflict.getStatus();
			result.message = currentConflict.getMessage(currentlyLoaded);
		}
		return result;
	}
	
	private final String doOperation(String operationType, T element) throws Exception {
		return operations.get(operationType).execute(element);
	}
}
