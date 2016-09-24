import $ from "./lib/jquery";
import TaskCache from "./tasklist/TaskCache";
import TaskList from "./tasklist/TaskList";
import TaskSwitcher from "./tasklist/TaskSwitcher";
import TaskEventBindings from "./tasklist/TaskEventBindings";
import { allVars } from "./shared/default";

/**
 * This function is executed when document is ready for interactivity!
 */
$(document).ready(
	function() {
		var taskCache = TaskCache({
			renderUrl: "/public/linkedTasks/renderTaskData",
			eventUrl: "/public/linkedTasks/taskDataEvents"
		});
		var taskSwitcher = TaskSwitcher();
		var taskBinders = TaskEventBindings(function(){});
		
		var taskListSettings = {
			taskListId : "linkedTasks",
			$list : $("#taskList"),
			eventUrl : "/public/linkedTasks/taskListEvents",
			eventBinders : taskBinders,
			onChange : null, // TODO add count for list
			currentUser : allVars.currentUser
		};
		var taskList = TaskList(taskListSettings, taskCache, taskSwitcher, {});
		taskList.update();
	}
);