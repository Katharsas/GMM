import TaskList from "./TaskList";
import EventListener from "../shared/EventListener";

var WorkbenchList = function(settings, cache, taskSwitcher) {
	
	var taskListEventHandlers = {
			
		FilterAll : function(event, funcs) {
			var newVisibleIds = event.visibleIdsOrdered;
			// remove tasks which became invisible
			var hidden = funcs.getCurrent().filter(function(id) {
				return newVisibleIds.indexOf(id) < 0;
			});
			return funcs.removeTasks(hidden, true)
			.then(function() {
				// add tasks which became visible
				var addedIds = newVisibleIds.diff(funcs.getCurrent());
				return funcs.addTasks(addedIds)
				// resort
				.then(function() {
					funcs.resortTasks(newVisibleIds);
				});
			});
		},
		
		SortAll : function(event, funcs) {
			funcs.resortTasks(event.visibleIdsOrdered);
			return Promise.resolve();
		},
		
		SortSingle : function(event, funcs) {
			funcs.moveTask(event.movedId, undefined, event.newPos);
			return Promise.resolve();
		}
	};
	
	var list = TaskList(settings, cache, taskSwitcher, taskListEventHandlers);

	EventListener.subscribe(EventListener.events.WorkbenchChangeEvent, list.update);
	
	// TODO: list should not need to listen for TaskDataChangeEvent
	// (currently the server does not send specific WorkbenchChangeEvent)
	EventListener.subscribe(EventListener.events.TaskDataChangeEvent, list.update);

	return list;
};

export default WorkbenchList;