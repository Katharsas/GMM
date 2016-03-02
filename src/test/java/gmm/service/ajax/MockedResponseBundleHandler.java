package gmm.service.ajax;

import gmm.collections.List;
import gmm.service.ajax.operations.MessageResponseOperations;

/**
 * 
 * @author Jan Mothes
 *
 * @see {@link BundledMessageResponses}
 */
public class MockedResponseBundleHandler<T> {

	/**
	 * Creates a BundledMessageResponses instance with the given operations, starts loading all elements
	 * and automatically answers to all responses. This only works as long as the rsponse is not a conflict.
	 * 
	 * @param elements - see {@link BundledMessageResponses}
	 * @param ops - see {@link BundledMessageResponses}
	 * @throws IllegalStateException if a response contains a conflict
	 */
	public void processResponses(Iterable<T> elements, MessageResponseOperations<T> ops) {
		final BundledMessageResponses<T> responses = new BundledMessageResponses<>(elements, ops);
		List<MessageResponse> list = responses.loadFirstBundle();
		while(true) {
			final String status = list.get(list.size()-1).getStatus();
			if (status.equals(BundledMessageResponses.finished)) {
				break;
			}
			else if (!status.equals(BundledMessageResponses.success)) {
				throw new IllegalStateException("Conflict response returned by BundledMessageResponses.\n"
						+ "The caller of this method must garantee that no conflicts occur!");
			}
			list = responses.loadNextBundle(BundledMessageResponses.nextElementOp, false);
		}
	}
}
