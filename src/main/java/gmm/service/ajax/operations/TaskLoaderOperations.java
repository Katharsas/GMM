package gmm.service.ajax.operations;

import java.util.HashMap;
import java.util.Map;

import gmm.domain.Task;
import gmm.domain.UniqueObject;
import gmm.service.Spring;
import gmm.service.data.DataAccess;

/**
 * @author Jan Mothes
 */
public class TaskLoaderOperations extends MessageResponseOperations<Task> {
	
	private final DataAccess data = Spring.get(DataAccess.class);
	
	private final Conflict<Task> no_conflict = MessageResponseOperations.cast(NO_CONFLICT);
	
	private final Conflict<Task> conflict = new Conflict<Task>() {
		@Override public String getStatus() {
			return "conflict";
		}
		@Override public String getMessage(Task element) {
			return "Conflict: A task with id: "+element.getId()+" already exists!";
		}
	};
	
	@Override
	public Map<String, Operation<Task>> getOperations() {
		Map<String, Operation<Task>> map = new HashMap<>();
		
		map.put("skip", new Operation<Task>() {
			@Override public String execute(Task element) {
				return "Skipping conflicting "+print(element);
			}
		});
		map.put("overwrite", new Operation<Task>() {
			@Override public String execute(Task element) {
				data.remove(element);
				data.add(element);
				return "Overwriting task with conflicting "+print(element);
			}
		});
		map.put("both", new Operation<Task>() {
			@Override public String execute(Task element) {
				element.makeUnique();
				data.add(element);
				return "Adding conflicting task as new "+print(element);
			}
		});
		return map;
	}
	
	@Override public Conflict<Task> onLoad(Task t) throws Exception {
		t.onLoad();
		UniqueObject.updateCounter(t);
		if(!data.add(t)) return conflict;
		else return no_conflict;
	}
	
	@Override public String onDefault(Task element) {
		return "Successfully added "+print(element);
	}

	private String print(Task t) {
		return "task [id: "+t.getId()+", name: "+t.getName()+"]";
	}
}