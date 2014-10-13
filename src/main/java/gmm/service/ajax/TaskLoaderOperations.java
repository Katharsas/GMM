package gmm.service.ajax;

import gmm.domain.Task;
import gmm.domain.UniqueObject;
import gmm.service.Spring;
import gmm.service.data.DataAccess;

public class TaskLoaderOperations extends MessageResponseOperations<Task> {
	private final DataAccess data = Spring.get(DataAccess.class);
	
	public TaskLoaderOperations() {
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
	}
	
	@Override public boolean onLoad(Task t) {
		t.onLoad();
		UniqueObject.updateCounter(t);
		return !data.add(t);
	}
	
	@Override public String onDefault(Task element) {
		return "Successfully added "+print(element);
	}
	
	@Override public String onConflict(Task element) {
		return "Conflict: A task with id: "+element.getId()+" already exists!";
	}
	
	private String print(Task t) {
		return "task [id: "+t.getId()+", name: "+t.getName()+"]";
	}
}