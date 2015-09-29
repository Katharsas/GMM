/* jshint esnext:true */
import $ from "./lib/jquery";
import Queue from "./shared/queue";
import TaskLoader from "./shared/taskloader";
import TaskSwitcher from "./shared/taskswitcher";
import TaskEventBindings from "./shared/tasklisteners";
import { allVars } from "./shared/default";

var tasksVars = {
	"selectedTaskFileIsAsset" : "",
	"expandedTasks" : undefined,
};

var tasksFuncs = {
	"subDir" : function() {
		return tasksVars.selectedTaskFileIsAsset ? "asset" : "other";
	},
	"filePath" : function() {
		return allVars.selectedTaskFile.attr("rel");
	}
};

/**
 * This function is executed when document is ready for interactivity!
 */
$(document).ready(
	function() {
		var taskListId = "linked";
		var taskLoader = TaskLoader;
		taskLoader.registerTaskList(taskListId, {
			$list : $("#taskList"),
			url : "/public/linkedTasks",
		});
		
		var taskSwitcher = TaskSwitcher(taskListId, taskLoader);
		var expandedTasks = new Queue(3, function($task1, $task2) {
				return $task1[0] === $task2[0];
		});
		
		var taskBinders = TaskEventBindings(tasksVars, tasksFuncs,
			function($task) {
				taskSwitcher.switchTask($task, expandedTasks);
			},
			function($task) {
				taskLoader.updateTask(taskListId, $task);
			},
			function($task) {
				taskLoader.removeTask($task);
			}
		);
		taskLoader.setTaskEventBinders(taskListId, taskBinders);
		
		var render = function() {
			taskLoader.createTaskList(taskListId, function() {
				//TODO: count element in html
//				var $count = $workbenchList.find(".list-count span");
//				$count.text(taskLoader.getTaskIds.length);
				this.expandedTasks.clear();
			});
		};
		render();
	}
);