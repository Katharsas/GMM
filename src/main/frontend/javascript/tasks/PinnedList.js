import TaskList from "./TaskList";
import EventListener from "../shared/EventListener";

var PinnedList = function(settings, cache, taskSwitcher) {
	
	var list = TaskList(settings, cache, taskSwitcher, {});
	
	EventListener.subscribe(EventListener.events.PinnedListChangeEvent, list.update);
	
	// TODO: list should not need to listen for TaskDataChangeEvent
	// (currently the server does not send specific PinnedListChangeEvents)
	EventListener.subscribe(EventListener.events.TaskDataChangeEvent, list.update);
	
	return list;
};

export default PinnedList;