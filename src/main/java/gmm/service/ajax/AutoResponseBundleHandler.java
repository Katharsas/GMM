package gmm.service.ajax;

import java.util.function.Function;

import org.springframework.stereotype.Service;

import gmm.collections.List;
import gmm.service.ajax.operations.ConflictChecker;

/**
 * 
 * @author Jan Mothes
 *
 * @see {@link BundledMessageResponses}
 */
@Service
public class AutoResponseBundleHandler<T> {

	/**
	 * Creates a BundledMessageResponses instance with the given operations, starts loading all
	 * elements and automatically answers to all responses. This only works as long as the response
	 * is not a conflict.
	 * 
	 * @param elements - see {@link BundledMessageResponses}
	 * @param ops - see {@link BundledMessageResponses}
	 * @param conflictHandler - Called if a conflict occurs, given the conflict status as parameter.
	 * @throws IllegalStateException if a response contains a conflict
	 */
	public void processResponses(Iterable<T> elements, ConflictChecker<T> ops,
			Function<MessageResponse, ConflictAnswer> conflictHandler) {
		
		final BundledMessageResponses<T> responses = new BundledMessageResponses<>(elements, ops);
		processResponses(responses, conflictHandler);
	}
	
	/**
	 * @see {@link #processResponses(Iterable, ConflictChecker, Function)}
	 */
	public void processResponses(BundledMessageResponsesProducer responses,
			Function<MessageResponse, ConflictAnswer> conflictHandler) {
		
		List<MessageResponse> list = responses.firstBundle();
		while(true) {
			final MessageResponse response = list.get(list.size()-1);
			final String status = response.getStatus();
			if (status.equals(BundledMessageResponses.finished)) {
				break;
			}
			else if (!status.equals(BundledMessageResponses.success)) {
				final ConflictAnswer answer = conflictHandler.apply(response);
				list = responses.nextBundle(answer);
			} else {
				list = responses.nextBundle(BundledMessageResponses.defaultAnswer);
			}
		}
	}
}
