package gmm.service.ajax.operations;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gmm.domain.UniqueObject;
import gmm.domain.task.Task;
import gmm.service.ajax.operations.ConflictChecker.Conflict;
import gmm.service.ajax.operations.ConflictChecker.Operation;
import gmm.service.data.DataAccess;

/**
 * @author Jan Mothes
 */
@Service
public class TaskIdConflictCheckerFactory {

	private final DataAccess data;
	
	@Autowired
	public TaskIdConflictCheckerFactory(DataAccess data) {
		this.data = data;
	}
	
	public TaskIdConflictChecker create() {
		return new TaskIdConflictChecker();
	}
	
	private final Conflict<Task> conflict = new Conflict<Task>() {
		@Override public String getName() {
			return "conflict";
		}
		@Override public String getDetails(Task element) {
			return "Conflict: A task with id: "+element.getId()+" already exists!";
		}
	};
	
	public Map<String, Operation<Task>> createOperations(DataAccess data) {
		final Map<String, Operation<Task>> map = new HashMap<>();
		
		map.put("skip", (conflict, element) -> {
			return "Skipping conflicting "+print(element);
		});
		map.put("overwrite", (conflict, element) -> {
			data.remove(element);
			data.add(element);
			return "Overwriting task with conflicting "+print(element);
		});
		map.put("both", (conflict, element) -> {
			element.makeUnique();
			data.add(element);
			return "Adding conflicting task as new "+print(element);
		});
		return map;
	}
	
	private String print(Task t) {
		return "task [id: "+t.getId()+", name: "+t.getName()+"]";
	}
	
	
	public class TaskIdConflictChecker extends ConflictChecker<Task> {
		
		private final Map<String, Operation<Task>> ops;
		
		private TaskIdConflictChecker() {
			ops = createOperations(data);
		}
		
		@Override
		public Map<String, Operation<Task>> getAllOperations() {
			return ops;
		}
		
		@Override public Conflict<Task> onLoad(Task t) {
			t.onLoad();
			UniqueObject.updateCounter(t);
			
			if (UniqueObject.getFromId(data.getList(t.getClass()), t.getId()) != null) {
				return conflict;
			} else {
				return NO_CONFLICT;
			}
		}
		
		@Override public String onDefault(Task element) {
			data.add(element);
			return "Successfully added "+print(element);
		}
	}
}