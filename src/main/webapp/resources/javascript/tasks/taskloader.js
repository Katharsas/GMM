/**
 * -------------------- TaskLoader ----------------------------------------------------------------
 * Static (called when document ready)
 * Accepts callback which will be executed when all tasks are loaded.
 */
var TaskLoader = function(url, $taskList, onLoaded) {
	this.tasks = undefined;
	reloadAndInsert();
	
	function reloadAndInsert() {
		$.getJSON(url).done(function(taskRenders) {
			tasks = taskRenders;
			insertHeaders();
			if(onLoaded !== undefined) onLoaded();
		}).fail(showException);
	}
	
	function insertHeaders() {
		var headerString = "";
		tasks.forEach(function (task) {
			headerString = headerString + task.header + "\n";
		});
		$taskList.append(headerString);
	}
	
	return {
		/**
		 * Insert the taskbody of the given task.
		 * Task must not have body already!
		 */
		insertBody : function ($task) {
			var idLink = $task.attr('id');
			var body = undefined;
			tasks.some(function(task) {
				if(task.idLink === idLink) {
					body = task.body;
					return true;
				}
				return false;
			});
			$task.append(body);
		},
		
		/**
		 * Remove the taskbody of the given task.
		 * Task must have a body!
		 */
		removeBody : function ($task) {
			$task.children(":last-child").remove();
		}
	};
};