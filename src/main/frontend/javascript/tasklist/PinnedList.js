import TaskList from "./TaskList";

var PinnedList = function(settings, cache, taskSwitcher) {
	
	var taskListEventHandlers = {
			
		AddAll : function(event, funcs) {
			for (let id of event.addedIds) {
				taskSwitcher.switchPinOperations(id, true);
			}
		},
		
		AddSingle : function(event, funcs) {
			taskSwitcher.switchPinOperations(event.addedId, true);
		},
		
		RemoveAll : function(event, funcs) {
			for (let id of event.removedIds) {
				taskSwitcher.switchPinOperations(id, false);
			}
		},
		
		RemoveSingle : function(event, funcs) {
			taskSwitcher.switchPinOperations(event.removedId, false);
		}
	};
	
	var list = TaskList(settings, cache, taskSwitcher, taskListEventHandlers);
	
	settings.eventBinders.setOnPinnedChange(list.update);
	taskSwitcher.setIsPinnedTask(list.contains);
	
	return list;
};

export default PinnedList;