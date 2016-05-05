import $ from "./lib/jquery";
import Queue from "./shared/queue";
import TaskLoader from "./shared/taskloader";
import TaskSwitcher from "./shared/taskswitcher";
import TaskEventBindings from "./shared/tasklisteners";

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
		
		var taskBinders = TaskEventBindings(
			function($task) {
				taskSwitcher.switchTask($task, expandedTasks);
			},
			function($task, id) {
				taskLoader.updateTask(taskListId, id);
			},
			function($task, id) {
				taskLoader.removeTask(id);
			}
		);
		taskLoader.setTaskEventBinders(taskListId, taskBinders);
		
		var render = function() {
			taskLoader.createTaskList(taskListId, function() {
				//TODO: count element in html
//				var $count = $workbenchList.find(".list-count span");
//				$count.text(taskLoader.getTaskIds.length);
				expandedTasks.clear();
			});
		};
		render();
	}
);