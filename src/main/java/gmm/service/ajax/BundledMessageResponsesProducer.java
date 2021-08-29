package gmm.service.ajax;

import gmm.collections.List;
import gmm.util.TypedString;

public interface BundledMessageResponsesProducer<O extends TypedString> {
	
	public List<MessageResponse> firstBundle();
	public List<MessageResponse> nextBundle(ConflictAnswer<O> answer);
	public ConflictAnswer<O> defaultAnswer();
	public ConflictAnswer<O> createAnswer(String operation, boolean doForAllFlag);
}
