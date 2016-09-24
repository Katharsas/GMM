import TaskList from "./TaskList";

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
					funcs.resortTaskList(newVisibleIds);
				});
			});
		},
		
		SortAll : function(event, funcs) {
			funcs.resortTaskList(event.visibleIdsOrdered);
			return Promise.resolve();
		},
		
		SortSingle : function(event, funcs) {
			funcs.moveTask(event.movedId, undefined, event.newPos);
			return Promise.resolve();
		}
	};
	
	var list = TaskList(settings, cache, taskSwitcher, taskListEventHandlers);
	return list;
};

export default WorkbenchList;