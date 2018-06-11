import $ from "./lib/jquery";
import { DataChangeNotifierInit } from "./shared/DataChangeNotifier";
import TaskCache from "./tasks/TaskCache";
import TaskList from "./tasks/TaskList";
import TaskSwitcher from "./tasks/TaskSwitcher";
import TaskEventBindings from "./tasks/TaskEventBindings";
import TaskDialogs, { TaskDialogsInit } from "./shared/TaskDialog";
import { allVars } from "./shared/default";
import {} from "./shared/template";

/**
 * This function is executed when document is ready for interactivity!
 */
$(document).ready(
	function() {

		DataChangeNotifierInit("/public/linkedTasks/taskDataEvents");

		var taskCache = TaskCache("/public/linkedTasks/renderTaskData");
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
			currentUser : allVars.currentUser
		};
		var taskList = TaskList(taskListSettings, taskCache, taskSwitcher, {});
		taskList.update();

		TaskDialogsInit(taskCache, taskBinders);
	}
);