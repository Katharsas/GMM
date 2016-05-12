package gmm.service.ajax;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import gmm.collections.LinkedList;
import gmm.collections.List;
import gmm.service.ajax.operations.ConflictChecker;
import gmm.service.ajax.operations.ConflictChecker.Conflict;
import gmm.service.ajax.operations.ConflictChecker.Operation;

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
public class BundledMessageResponses<T> implements BundledMessageResponsesProducer {
	
	private final Runnable onFinished;
	private final ConflictChecker<T> ops;
	private final Map<String, Operation<T>> operations;
	
	/**
	 * A conflict gets added if the user checks the "doForAll" button.
	 * Conflicts are mapped to the operation the user chose when he checked the button.
	 */
	private final Map<Conflict<T>, String> doForAlls = new HashMap<>();
	private final Iterator<? extends T> elements;
	private Conflict<T> currentConflict;
	private T currentlyLoaded;
	
	// Operation provided from the client if client has nothing to say.
	// Other operations are conflict handler ids of a specific MessageResponseOperations object.
	// TODO: wrap operation id into seperate class
	private static final String nextElementOp = "default";
	
	// Two of thre possible answers to the client, the third being a specific conflict id.
	// TODO: create enum with SUCCESS, FINISHED, CONFLICT and move conflict type into subfield
	protected static final String success = "success";
	public static final String finished = "finished";
	
	protected static final ConflictAnswer defaultAnswer = new ConflictAnswer(nextElementOp, false);
	
	public BundledMessageResponses(
			Iterable<? extends T> elements, ConflictChecker<T> ops) {
		this(elements, ops, ()->{});
	}
	
	public BundledMessageResponses(
			Iterable<? extends T> elements, ConflictChecker<T> ops, Runnable onFinished) {
		this.elements = elements.iterator();
		this.ops = ops;
		this.onFinished = onFinished;
		operations = ops.getAllOperations();
		currentConflict = ops.NO_CONFLICT;
	}
	
	@Override
	public List<MessageResponse> firstBundle() {
		return nextBundle(defaultAnswer);
	}
	
	@Override
	public List<MessageResponse> nextBundle(ConflictAnswer answer) {
		final List<MessageResponse> results = new LinkedList<>(MessageResponse.class);
		MessageResponse result = loadNext(answer);
		boolean loadNext = result.getStatus().equals(success);
		results.add(result);
		final long timeStamp = System.currentTimeMillis();
		while(loadNext) {
			result = loadNext(defaultAnswer);
			results.add(result);
			final long duration = System.currentTimeMillis() - timeStamp;
			loadNext = result.getStatus().equals(success) && duration < 2000;
		}
		return results;
	}
	
	private MessageResponse loadNext(ConflictAnswer answer) {
		//If the user wants to process a new element, we try to do so.
		if(answer.operation.equals(nextElementOp)) {
			return processNewElement();
		}
		//Else, he gave an answer on how to handle the last (conflicting) element.
		else {
			if(answer.doForAllFlag) doForAlls.put(currentConflict, answer.operation);
			return resolveConflict(answer.operation);
		}
	}
	
	private MessageResponse processNewElement() {
		//If loading finished, user should stop sending requests!
		if (!elements.hasNext()) {
			onFinished.run();
			return new MessageResponse(finished, null);
		} else {
			//We try to add the next element, which may cause a conflict.
			currentlyLoaded = elements.next();
			currentConflict = ops.onLoad(currentlyLoaded);
			final boolean isConflict = !currentConflict.equals(ops.NO_CONFLICT);
			
			// If conflict, we either use a previously given doForAll operation or ask the user.
			if(isConflict) {
				if (doForAlls.containsKey(currentConflict)) {
					return resolveConflict(doForAlls.get(currentConflict));
				} else {
					final String status = currentConflict.getName();
					final String message = currentConflict.getDetails(currentlyLoaded);
					return new MessageResponse(status, message);
				}	
			}
			//If not, we just add/process the new element and tell the user everything is fine.
			else {
				final String message = ops.onDefault(currentlyLoaded);
				return new MessageResponse(success, message);
			}
		}
	}
	
	private MessageResponse resolveConflict(String operation) {
		final String message = doOperation(operation, currentConflict, currentlyLoaded);
		return new MessageResponse(success, message);
	}
	
	private final String doOperation(String operationType, Conflict<T> conflict, T element) {
		return operations.get(operationType).execute(conflict, element);
	}
}
