/* jshint esnext:true */
import $ from "../lib/jquery";
import Ajax from "./ajax";
import HtmlPreProcessor from "./preprocessor";
import { contextUrl } from "./default";
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
	 * Asynchronous!
	 * Get list of tasks to show, load any missing task data, reinsert headers.
	 * @return - Promise
	 */
	var create = function(taskListId) {
		var taskList = taskListMap[taskListId];
		return $.when().then(function(){
				return updateVisibleAndCache(taskListId);
		}).then(function() {
				reinsertHeaders(taskListId, taskList.current);
		});
	};
	
	/**
	 * Asynchronous!
	 * Update tasks from server for all task lists.
	 * @return - Promise
	 */
	var update = function(taskListId, idLinks) {
		return $.when().then(function(){
			return loadIntoCache(taskListId, idLinks);
		}).then(function() {
			Object.keys(taskListMap).forEach(function(taskListId) {
				reinsertHeaders(taskListId, idLinks);
			});
		});
	};
	
	/**
	 * Remove tasks for all task lists.
	 */
	var remove = function(idLinks) {
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
	};
	
	/**
	 * Clears task list, then reinsert headers from cache into task list.
	 * ALL tasks are replaced to update order changes correctly.
	 */
	var reinsertHeaders = function(taskListId, idLinks) {
		var taskList = taskListMap[taskListId];
		taskList.$list.children(".task").remove();
		taskList.current.forEach(function(id) {
			var $header = idToTaskData[id].$header.clone(true, true);
			taskList.eventBinders.bindHeader($header);
			taskList.$list.append($header);
		});
	};
	
	/**
	 * Asynchronous!
	 * Get list of currently visible tasks
	 * @return - Promise
	 */
	var updateVisibleAndCache = (function() {return function(taskListId) {
			var taskList = taskListMap[taskListId];
			return Ajax.get(contextUrl + taskList.url + "/currentTaskIds")
				.then(function(taskListState) {
					taskList.current = taskListState.visibleIds;
					var dirtyIds = taskListState.dirtyIds;
					return $.when().then(function() {
						return loadIntoCache(taskListId, dirtyIds);
					}).then(function() {
						return getNotYetChachedIds(taskListId);
					}).then(function(missing) {
						return loadIntoCache(taskListId, missing);
					});
				});
		};
		/**
		 * Asynchronous!
		 * Get the ids of the tasks whose cache data is missing.
		 * @return - Promise
		 */
		function getNotYetChachedIds(taskListId) {
			var missing = [];
			var taskList = taskListMap[taskListId];
			taskList.current.forEach(function(id) {
				if (!idToTaskData.hasOwnProperty(id)) {
					missing.push(id);
				}
			});
			return missing;
		}
	})();
	
	/**
	 * Asynchronous!
	 * Gets updated task data from server and inserts processed data into cache.
	 * @return - Promise
	 */
	var loadIntoCache = function(taskListId, idLinks) {
		var taskList = taskListMap[taskListId];
		var url = contextUrl + taskList.url + "/renderTaskData";
		return Ajax.post(url, { "idLinks[]" : idLinks })
			.then(function (taskRenders) {
				taskRenders.forEach(function(task) {
					preprocess(task);
					idToTaskData[task.idLink] = task;
				});
			});
	};
	
	/**
	 * - converts html strings to dom elements
	 * - converts svg links to svg code
	 * - hides bodies
	 */
	var preprocess = function(task) {
		task.$header = $(task.header);
		delete task.header;
		HtmlPreProcessor.apply(task.$header);
		
		//asynch to not block GUI
		setTimeout(function() {
			task.$body = $(task.body);
			delete task.body;
			HtmlPreProcessor.apply(task.$body);
			task.$body.hide();
		}, 0);
	};
	
	var callIfExists = function(callback) {
		if (callback !== undefined) { 
			callback();
		}
	};
	
	return {
		
		registerTaskList : function(taskListId, settings) {
			settings.current = [];
			taskListMap[taskListId] = settings;
		},
		
		createTaskList : function(taskListId, callback) {
			create(taskListId).then(callback);
		},
		
		setTaskEventBinders : function(taskListId, eventBinders) {
			taskListMap[taskListId].eventBinders = eventBinders;
		},
		
		updateTask : function(taskListId, idLink) {
			update(taskListId, [idLink]);
		},
		
		removeTask : function(idLink) {
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
