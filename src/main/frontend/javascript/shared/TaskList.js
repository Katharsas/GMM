/*jshint loopfunc: true */

import Ajax from "./ajax";
import Dialogs from "./dialogs";
import { contextUrl, resortElementsById, runSerial } from "./default";
import TaskSwitcher from "./taskswitcher";

/**
 * @author Jan Mothes
 * 
 * @typedef TaskListSettings
 * @property {JQuery} $list - The container element which holds all task elements as children.
 * @property {string} eventUrl - The url path providing taskList events for synchronization.
 * @property {Callback} onChange - Executed whenever the taskList has changed. Can be null.
 * 		Count of current tasks will be passed as parameter.
 * @property {TaskEventBindings} eventBinders - Contains all functions for event binding.
 * 
 * @param {TaskListSettings} settings - Settings needed to create this taskList.
 * @param {TaskCache} cache - Task data container.
 */
var TaskList = function(settings, cache) {
	
	var taskSelector = ".task:not(.removed)";
	
	/**
	 * Ids of tasks which are currently visible.
	 */
	var current = [];
	
	/**
	 * Allows to expand/collapse task body on click.
	 */
	var taskSwitcher = TaskSwitcher({
		
			createBody : function($task) {
				var idLink = $task.attr('id');
				var $body = cache.getTaskBody(idLink);
				settings.eventBinders.bindBody(idLink, $task, $body, markDeprecated);
				return $body;
			},
			
			destroyBody : function($body) {
				$body.remove();
			}
		}
	);
	
	settings.eventBinders.bindList(settings.$list, function($task) {
		taskSwitcher.switchTask($task);
	});
	
	var findTask = function(idLink) {
		if (current.indexOf(idLink) < 0) return null;
		else {
			return settings.$list.children(taskSelector + "#" + idLink);
		}
	};
	
	
	var getHeader = function(idLink) {
		var $header = cache.getTaskHeader(idLink);
		settings.eventBinders.bindHeader($header);
		return $header;
	};
	
	/**
	 * Appends given tasks to list. Task data for must exit in cache.
	 * Does not check if task header has already been added to the list.
	 */
	var appendTaskHeaders = function(idLinks) {
		for(let id of idLinks) {
			var $header = getHeader(id);
			settings.$list.append($header);
		}
	};
	
	/**
	 * Async. Remove task from current ids array and from list (after collapsing if expanded).
	 * @returns {Promise}
	 */
	var removeTask = function($task, idLink, isExpanded) {
		if(isExpanded === undefined) {
			isExpanded = taskSwitcher.isTaskExpanded($task);
		}
		$task.addClass("removed");
		current.splice(current.indexOf(idLink), 1);
		return (isExpanded ? taskSwitcher.collapseTaskIfExpanded($task) : Promise.resolve())
		.then(function() {
			$task.remove();
		});
	};
	
	/**
	 * Async.
	 * @returns {Promise}
	 */
	var markDeprecated = function($task, idLink) {
		if ($task === null) $task = findTask(idLink);
		return removeTask($task, idLink)
		.then(updateTaskList);
	};
	
	/**
	 * Async. Get list of currently visible tasks.
	 * @returns {Promise}
	 */
	var updateTaskList = function() {
		return Ajax.get(contextUrl + settings.eventUrl)
		.then(function(taskListEvents) {
			var eventHandlers = [];
			for(let event of taskListEvents) {
				eventHandlers.push(function() {
					return taskListEventHandlers[event.eventName](event);
				});
			}
			return runSerial(eventHandlers);
		})
		.then(function() {
			if (settings.onChange !== null) {
				settings.onChange(current.length);
			}
		});
	};
	
	var getIdOfTask = function(task) {return task.id;};
	var resortTaskList = function(visibleIdsOrdered) {
		resortElementsById(visibleIdsOrdered, settings.$list, taskSelector, getIdOfTask);
	};
	
	var taskListEventHandlers = {
			
		FilterAll : function(event) {
			var newVisibleIds = event.visibleIdsOrdered;
			// load missing into cache
			var missing = cache.getMissingIds(newVisibleIds);
			return cache.loadTasks(missing)
			.then(function() {
				// add not visible from cache to page
				var addedIds = newVisibleIds.diff(current);
				appendTaskHeaders(addedIds);
				// remove hidden from page
				var hidden = current.filter(function(id) {
					return newVisibleIds.indexOf(id) < 0;
				});
				for (let id of hidden) {
					removeTask(findTask(id), id);
				}
				// resort page elements by visible
				resortTaskList(newVisibleIds);
				current = newVisibleIds;
			});
		},
		
		SortAll : function(event) {
			// resort page elements by visible
			resortTaskList(event.visibleIdsOrdered);
			current = event.visibleIdsOrdered;
			return Promise.resolve();
		},
		
		CreateAll : function(event) {
			// add createIds into cache and to page (need function)
			return cache.loadTasks(event.createdIds)
			.then(function() {
				appendTaskHeaders(event.createdIds);
				// resort page elements by visible and set current
				resortTaskList(event.visibleIdsOrdered);
				current = event.visibleIdsOrdered;
			});
		},
		
		CreateSingle : function(event) {
			var id = event.createdId;
			// add id html into cache
			return cache.loadTasks([id])
			// add html to page at given pos
			.then(function() {
				var $header = getHeader(id);
				var pos = event.insertedAtPos;
				settings.$list.children(taskSelector).eq(pos).before($header);
				current.splice(pos, 0, id); 
			});
		},
		
		RemoveAll : function(event) {
			var promises = [];
			for (let id of event.removedIds) {
				promises.push(this.RemoveSingle({
					eventName : "RemoveSingle",
					removedId : id
				}));
			}
			return Promise.all(promises);
		},
		
		RemoveSingle : function(event) {
			var id = event.removedId;
			// delete from cache
			cache.deleteTask(id);
			// check if task is currently visible
			if(current.indexOf(id) >= 0) {
				var $task = findTask(id);
				// remove task
				var isExpanded = taskSwitcher.isTaskExpanded($task);
				var promise = removeTask($task, id, isExpanded);
				// if expanded, explain to user why task vanished
				if (isExpanded) {
					var $confirm = Dialogs.alert(function() {
						Dialogs.hideDialog($confirm);
					}, "A task you had selected has been deleted or updated!");
				}
				return promise;
			}
			return Promise.resolve();
		}
	};
	
	
	return {
		
		/**
		 * Call this to cause this list to get updates from the server and thus sync it.
		 */
		update : function() {
			updateTaskList();
		},
		
		size : function() {
			return current.length;
		},
		
		/**
		 * Call this to remove a task from the list when the task or its data become outdated.
		 * Also triggers tasklist update.
		 */
		markTaskDeprecated : markDeprecated
		
	};
};

export default TaskList;