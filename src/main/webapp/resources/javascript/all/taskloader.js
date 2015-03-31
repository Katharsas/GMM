/**
 * -------------------- TaskLoader ----------------------------------------------------------------
 * Static (called when document ready)
 * Accepts callback which will be executed when all tasks are loaded.
 * 
 * TODO: "tasks" variable SCOPE ??
 * 
 * @author Jan Mothes
 */
var TaskLoader = function(url, $taskList, onLoaded) {
//	this.tasks = undefined;
//	reloadAndInsert();
	
	$count = $taskList.find(".list-count span");
	
	function reloadAndInsert() {
		Ajax.get(url).done(function(taskRenders) {
			tasks = taskRenders;
			insertHeaders();
			if(onLoaded !== undefined) onLoaded();
		});
	}
	
	function insertHeaders() {
		var headerString = "";
		tasks.forEach(function (task) {
			headerString = headerString + task.header + "\n";
		});
		$taskList.append(headerString);
		$count.text(tasks.length);
	}
	
	return {
		init : function() {
			$taskList.children().not(":first").remove();
			this.tasks = undefined;
			reloadAndInsert();
		},
		
		getBody : function($task) {
			return $task.children(":last-child");
	    },
		
		/**
		 * Insert the taskbody of the given task.
		 * Task must not have body already!
		 */
		insertBody : function ($task) {
			var idLink = $task.attr('id');
			var body = null;
			tasks.some(function(task) {
				if(task.idLink === idLink) {
					body = task.body;
					return true;
				}
				return false;
			});
			if (body !== null) {
				$task.append(body);
			} else {
				alert(undefined, "TaskLoader# insertBody(): No body found!");
			}
		},
		
		/**
		 * Remove the taskbody of the given task.
		 * Task must have a body!
		 */
		removeBody : function ($body) {
			$body.remove();
		}
	};
};