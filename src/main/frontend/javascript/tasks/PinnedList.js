import TaskList from "./TaskList";
import EventListener from "../shared/EventListener";

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
	
	EventListener.subscribe(EventListener.events.PinnedListChangeEvent, list.update);
	
	// TODO: list should not need to listen for TaskDataChangeEvent
	// (currently the server does not send specific PinnedListChangeEvents)
	EventListener.subscribe(EventListener.events.TaskDataChangeEvent, list.update);

	taskSwitcher.setIsPinnedTask(list.contains);
	
	return list;
};

export default PinnedList;