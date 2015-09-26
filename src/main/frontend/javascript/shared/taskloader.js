/* jshint esnext:true */
import $ from "../lib/jquery";
import Ajax from "./ajax";
import HtmlPreProcessor from "./preprocessor";
import { contextUrl, allFuncs } from "./default";
//missing: taskVars

/**
 * -------------------- TaskLoader ----------------------------------------------------------------
 * Static (called when document ready)
 * All task lists must be registered using the taskListMap parameter object.
 * The Taskloader can get the task lists, task data and insert the headers into dom.
 * 
 * 
 * @author Jan Mothes
 */
export default (function() {
	
	/**
	 * Cache, which maps task ids to task nodes:
	 * idString => task { idString, $header, $body }
	 */
	var idToTaskData = {};
	
	/**
	 * Maps a taskListId to a taskList settings object:
	 * taskListId => { $list, url, onchange, current[], eventListeners }
	 * 
	 * 		$list - the jquery list container for the task nodes
	 * 		url - the part of the url which is specific for this taskList
	 * 		onchange - callback that gets executed when task list changes
	 * 		current - set by TaskLoader, array of currently visible tasks ids
	 * 		eventBinders - callbacks that add event bindings to tasks {
	 * 			bindHeader : function($task),
	 * 			bindBody : function(id, $body)
	 * 		}
	 */
	var taskListMap = {};
	
	/**
	 * Get list of tasks to show, load any missing task data, reinsert headers.
	 */
	function create(taskListId, done) {
		var taskList = taskListMap[taskListId];
		
		getCurrent(taskListId, function() {
		loadMissingTaskData(taskListId, function() {
		reinsertHeaders(taskListId, taskList.current);
		});});
	}
	
	/**
	 * Update tasks from server for all task lists.
	 */
	function update(taskListId, idLinks, done) {
		updateCacheTaskData(taskListId, idLinks, function() {
		Object.keys(taskListMap).forEach(function(taskListId) {
			reinsertHeaders(taskListId, idLinks);
		});
		callIfExists(done);
		});
	}
	
	/**
	 * Remove tasks for all task lists.
	 */
	function remove (idLinks) {
		//delete from cache
		idLinks.forEach(function(id) {
			delete idToTaskData[id];
		});
		//delete from current & from list itself
		Object.keys(taskListMap).forEach(function(taskListId) {
			var taskList = taskListMap[taskListId];
			var $tasks = taskList.$list.children(".task");
			idLinks.forEach(function(id) {
				taskList.current.forEach(function(currentId, index) {
					if(id === currentId) taskList.current.splice(index, 1);
				});
				$tasks.remove("#"+id);
			});
		});
	}
	
	/**
	 * Clears task list, then reinsert headers from cache into task list.
	 * ALL tasks are replaced to update order changes correctly.
	 */
	function reinsertHeaders(taskListId, idLinks) {
		var taskList = taskListMap[taskListId];
		taskList.$list.children(".task").remove();
		taskList.current.forEach(function(id) {
			var $header = idToTaskData[id].$header.clone(true, true);
			taskList.eventBinders.bindHeader($header);
			taskList.$list.append($header);
		});
	}
	
	/**
	 * Get list of currently visible tasks
	 */
	function getCurrent(taskListId, done) {
		var taskList = taskListMap[taskListId];
		Ajax.get(contextUrl + taskList.url + "/currentTaskIds")
			.done(function(tasks) {
				taskList.current = tasks;
				callIfExists(done);
			});
	}
	
	/**
	 * Load task data for any current tasks that is not cached yet.
	 */
	function loadMissingTaskData(taskListId, done) {
		var missing = [];
		var taskList = taskListMap[taskListId];
		taskList.current.forEach(function(id) {
			if (!idToTaskData.hasOwnProperty(id)) {
				missing.push(id);
			}
		});
		updateCacheTaskData(taskListId, missing, done);
	}
	
	/**
	 * Gets updated task data from server and inserts processed data into cache.
	 */
	function updateCacheTaskData(taskListId, idLinks, done) {
		var taskList = taskListMap[taskListId];
		Ajax.post(contextUrl + taskList.url + "/renderTaskData", { "idLinks[]" : idLinks })
			.done(function (taskRenders) {
				taskRenders.forEach(function(task) {
					preprocess(task);
					idToTaskData[task.idLink] = task;
				});
				callIfExists(done);
			});
	}
	
	/**
	 * - converts html strings to dom elements
	 * - converts svg links to svg code
	 * - hides bodies and inserts filetrees into asset task bodies
	 */
	function preprocess(task) {
		task.$header = $(task.header);
		delete task.header;
		HtmlPreProcessor.apply(task.$header);
		
		//asynch to not block GUI
		setTimeout(function() {
			task.$body = $(task.body);
			delete task.body;
			HtmlPreProcessor.apply(task.$body);
			task.$body.hide();
			var id = task.idLink;
			task.$body.find('.task-files-assets-tree').fileTree(
				allFuncs.treePluginOptions(contextUrl + "/tasks/files/assets/" + id, false),
				function($file) {
					tasksVars.selectedTaskFileIsAsset = true;
					allFuncs.selectTreeElement($file, "task-files-selected");
				}
			);
			task.$body.find('.task-files-other-tree').fileTree(
				allFuncs.treePluginOptions(contextUrl + "/tasks/files/other/" + id, false),
				function($file) {
					tasksVars.selectedTaskFileIsAsset = false;
					allFuncs.selectTreeElement($file, "task-files-selected");
				}
			);
		}, 0);
	}
	
	function callIfExists(callback) {
		if (callback !== undefined) { 
			callback();
		}
	}
	
	return {
		
		registerTaskList : function(taskListId, settings) {
			settings.current = [];
			taskListMap[taskListId] = settings;
		},
		
		createTaskList : function(taskListId, callback) {
			create(taskListId, callback);
		},
		
		setTaskEventBinders : function(taskListId, eventBinders) {
			taskListMap[taskListId].eventBinders = eventBinders;
		},
		
		updateTask : function(taskListId, $task) {
			var idLink = $task.attr('id');
			update(taskListId, [idLink]);
		},
		
		removeTask : function($task) {
			var idLink = $task.attr('id');
			remove([idLink]);
		},
		
		removeTasks : function(idLinks) {
			remove(idLinks);
		},
		
		getTaskIds : function(taskListId) {
			return taskListMap[taskListId].current;
		},

		/**
		 * Insert the taskbody of the given task.
		 * Task must not have body already!
		 */
		insertBody : function (taskListId, $task) {
			var idLink = $task.attr('id');
			var task = idToTaskData[idLink];
			var $body = task.$body.clone(true, true);
			taskListMap[taskListId].eventBinders.bindBody(idLink, $task, $body);
			$task.append($body);
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
})();