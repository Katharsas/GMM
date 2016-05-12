package gmm.service.ajax;

import gmm.collections.List;

public interface BundledMessageResponsesProducer {
	
	public List<MessageResponse> firstBundle();
	public List<MessageResponse> nextBundle(ConflictAnswer answer);
}
