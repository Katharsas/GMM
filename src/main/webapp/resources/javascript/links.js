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

//add listeners to global scope
var listeners = TaskListeners(tasksVars, tasksFuncs);
for (var func in listeners) {
	window[func] = listeners[func];
}

/**
 * This function is executed when document is ready for interactivity!
 */
$(document).ready(
	function() {
		TaskLoader = TaskLoader(allVars.contextPath+"/public/linkTasks/render", $("#taskList"));
		TaskSwitcher = TaskSwitcher(TaskLoader);
		tasksVars.expandedTasks = new Queue(3, function($task1, $task2) {
				return $task1[0] === $task2[0];
		});
	}
);

function switchListElement(element) {
	TaskSwitcher.switchTask($(element).parent().first(), tasksVars.expandedTasks);
}