import $ from "./lib/jquery";
import TaskCache from "./shared/TaskCache";
import TaskList from "./shared/TaskList";
import TaskEventBindings from "./shared/tasklisteners";

/**
 * This function is executed when document is ready for interactivity!
 */
$(document).ready(
	function() {
		
		var taskBinders = TaskEventBindings(function(){});
		var taskCache =  TaskCache("/public/linkedTasks/renderTaskData");
		
		var taskListSettings = {
			taskListId : "linkedTasks",
			$list : $("#taskList"),
			eventUrl : "/public/linkedTasks/taskListEvents",
			eventBinders : taskBinders,
			onChange : null // TODO add count for list
		};
		var taskList = TaskList(taskListSettings, taskCache);
		taskList.update();
	}
);