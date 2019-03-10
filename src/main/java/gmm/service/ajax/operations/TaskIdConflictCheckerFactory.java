package gmm.service.ajax.operations;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

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
	
	public TaskIdConflictChecker create(Consumer<Task> onAdd) {
		return new TaskIdConflictChecker(onAdd);
	}
	
	private final Conflict<Task> conflict = new Conflict<Task>() {
		@Override public String getName() {
			return "conflict";
		}
		@Override public String getDetails(Task element) {
			return "Conflict: A task with id: "+element.getId()+" already exists!";
		}
	};
	
	public Map<String, Operation<Task>> createOperations(DataAccess data, Consumer<Task> onAdd) {
		final Map<String, Operation<Task>> map = new HashMap<>();
		
		map.put("skip", (conflict, element) -> {
			return "Skipping this conflicting "+print(element);
		});
		map.put("overwrite", (conflict, element) -> {
			data.remove(element);
			onAdd.accept(element);
			return "Overwriting existing task with this "+print(element);
		});
		map.put("both", (conflict, element) -> {
			element.makeUnique();
			onAdd.accept(element);
			return "Adding this as new "+print(element);
		});
		return map;
	}
	
	private String print(Task t) {
		return "task [id: "+t.getId()+", name: "+t.getName()+"]";
	}
	
	
	public class TaskIdConflictChecker extends ConflictChecker<Task> {
		
		private final Map<String, Operation<Task>> ops;
		private final Consumer<Task> onAdd;
		
		private TaskIdConflictChecker(Consumer<Task> onAdd) {
			ops = createOperations(data, onAdd);
			this.onAdd = onAdd;
		}
		
		@Override
		public Map<String, Operation<Task>> getAllOperations() {
			return ops;
		}
		
		@Override public Conflict<Task> onLoad(Task t) {
			UniqueObject.updateCounter(t);
			
			if (UniqueObject.getFromId(data.getList(t.getClass()), t.getId()) != null) {
				return conflict;
			} else {
				return NO_CONFLICT;
			}
		}
		
		@Override public String onDefault(Task element) {
			onAdd.accept(element);
			return "Successfully added "+print(element);
		}
	}
}
