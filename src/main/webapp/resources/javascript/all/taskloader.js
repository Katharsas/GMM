/**
 * -------------------- TaskLoader ----------------------------------------------------------------
 * Static (called when document ready)
 * Accepts callback which will be executed when all tasks are loaded.
 * 
 * TODO: "tasks" variable SCOPE ??
 * TODO: see preprocess method
 * 
 * @author Jan Mothes
 */
var TaskLoader = function(url, $taskList, onLoaded) {
	var tasks =null;
	
	$count = $taskList.find(".list-count span");
	
	function reloadAndInsertHeaders() {
		Ajax.get(url).done(function(taskRenders) {
			tasks = taskRenders;
			tasks.forEach(function(task) {
				preprocess(task);
				$taskList.append(task.header);
			});
			$count.text(tasks.length);
			if(onLoaded !== undefined) onLoaded();
		});
	}
	
	/**
	 * - converts html strings to dom elements
	 * - converts svg links to svg code
	 * - hides bodies and inserts filetrees into asset task bodies
	 */
	function preprocess(task) {
		task.header = $(task.header);
		allVars.htmlPreProcessor.apply(task.header);
		
		//asynch to not block GUI
		setTimeout(function() {
			task.body = $(task.body);
			allVars.htmlPreProcessor.apply(task.body);
			task.body.hide();
			var url = task.idLink;
			task.body.find('#assetFilesContainer').fileTree(
				allFuncs.treePluginOptions(contextUrl + "/tasks/files/assets/" + url, false),
				function($file) {
					tasksVars.selectedTaskFileIsAsset = true;
					allFuncs.selectTreeElement($file, "selectedTaskFile");
				}
			);
			task.body.find('#wipFilesContainer').fileTree(
				allFuncs.treePluginOptions(contextUrl + "/tasks/files/other/" + url, false),
				function($file) {
					tasksVars.selectedTaskFileIsAsset = false;
					allFuncs.selectTreeElement($file, "selectedTaskFile");
				}
			);
		}, 0);
	}
	
	return {
		init : function() {
			$taskList.children().not(":first").remove();
			reloadAndInsertHeaders();
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
			var $body = null;
			tasks.some(function(task) {
				if(task.idLink === idLink) {
					$body = $(task.body);
					return true;
				}
				return false;
			});
			if ($body !== null) {
				$task.append($body);
			} else {
				alert(undefined, "TaskLoader# insertBody(): No body found!");
			}
		},
		
		/**
		 * Remove the taskbody of the given task.
		 * Task must have a body!
		 */
		removeBody : function ($body) {
			$body.hide();
			$body.remove();
		}
	};
};