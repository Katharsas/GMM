var expandedTasks = undefined;


/**
 * This function is executed when document is ready for interactivity!
 */
$(document).ready(
	function() {			
		TaskLoader = TaskLoader("render", $("#listsMain"));
		TaskSwitcher = TaskSwitcher(TaskLoader);
		expandedTasks = new Queue(3, function($task1, $task2) {
			return $task1[0] === $task2[0];
	});
});

function switchListElement(element) {
	TaskSwitcher.switchTask($(element).parent().first(), expandedTasks);
}