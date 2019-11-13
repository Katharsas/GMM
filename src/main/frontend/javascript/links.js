import $ from "./lib/jquery";
import { DataChangeNotifierInit } from "./shared/DataChangeNotifier";
import TaskCache from "./tasks/TaskCache";
import TaskList from "./tasks/TaskList";
import TaskSwitcher from "./tasks/TaskSwitcher";
import TaskEventBindings from "./tasks/TaskEventBindings";
import EventListener from "./shared/EventListener";
import { TaskDialogsInit } from "./shared/TaskDialog";
import { allVars } from "./shared/default";
import {} from "./shared/template";

/**
 * This function is executed when document is ready for interactivity!
 */
$(document).ready(
	function() {

		DataChangeNotifierInit("/public/linkedTasks/taskDataEvents");
		
		var taskCache;
		if (allVars.isUserLoggedIn) {
			taskCache = TaskCache("/public/linkedTasks/renderTaskDataAny");
		} else {
			taskCache = TaskCache("/public/linkedTasks/renderTaskData");
		}
		
		var taskSwitcher = TaskSwitcher();
		var taskBinders = TaskEventBindings(
			function(){}, taskCache.triggerIsPinned, taskCache.isPinned);
		
		var taskListSettings = {
			taskListId : "linkedTasks",
			$list : $("#taskList"),
			eventUrl : "/public/linkedTasks/taskListEvents",
			initUrl : null,
			eventBinders : taskBinders,
			onUpdateDone : null, // TODO add count for list
			currentUser : allVars.currentUser,
			expandSingleTask : true
		};
		var taskList = TaskList(taskListSettings, taskCache, taskSwitcher, {});
		
		// TODO: list should not need to listen for TaskDataChangeEvent
		// (currently the server does not send specific PublicListChangeEvent)
		EventListener.subscribe(EventListener.events.TaskDataChangeEvent, taskList.update);

		TaskDialogsInit(taskCache, taskBinders);
	}
);