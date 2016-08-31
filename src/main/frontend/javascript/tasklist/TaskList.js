/*jshint loopfunc: true */

import Ajax from "../shared/ajax";
import Dialogs from "../shared/dialogs";
import { contextUrl, resortElementsById, runSerial } from "../shared/default";

/**
 * @author Jan Mothes
 * 
 * @typedef TaskListSettings
 * @property {JQuery} $list - The container element which holds all task elements as children.
 * @property {string} eventUrl - The url path providing taskList events for synchronization.
 * @property {Callback} onChange - Executed whenever the taskList has changed. Can be null.
 * 		Count of current tasks will be passed as parameter.
 * @property {TaskEventBindings} eventBinders - Contains all functions for event binding.
 * @property {UserId} currentUser - Current user or null if user is not logged in.
 * 
 * @param {TaskListSettings} settings - Settings needed to create this taskList.
 * @param {TaskCache} cache - Task data container.
 */
var TaskList = function(settings, cache, taskSwitcher) {
	
	var taskListId = settings.taskListId;
	var taskSelector = ".task:not(.removed)";
	
	/**
	 * Ids of tasks which are currently visible.
	 */
	var current = [];
	
	
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
	 * Append given tasks to list. Task data for must exit in cache.
	 * Does not check if task header has already been added to the list.
	 */
	var appendTaskHeaders = function(idLinks) {
		for(let id of idLinks) {
			var $header = getHeader(id);
			settings.$list.append($header);
			current.push(id);
			taskSwitcher.expandIfWanted($header, id, taskListId, true);
		}
	};
	
	/**
	 * Move given task from given old position to new position.
	 * @param oldPos - Old position of the task or undefined if unknown.
	 */
	var moveTask = function(idLink, oldPos, newPos) {
		if (typeof oldPos === "undefined") {
			oldPos = current.indexOf(idLink);
		}
		var $task = findTask(idLink);
		$task.detach();
		current.splice(oldPos, 1);
		// insert before element or at end
		if(newPos !== current.length) {
			settings.$list.children(taskSelector).eq(newPos).before($task);
		} else {
			settings.$list.append($task);
		}
		current.splice(newPos, 0, idLink);
	};
	
	/**
	 * Async. Remove task from current ids array and from list (after collapsing if expanded).
	 * @param {bool} isExpanded - Should be undefined if unknown (will be figured out by this function).
	 * @param {bool} instantly - Wether an expanded task should collapse instantly or animated.
	 * @returns {Promise}
	 */
	var removeTask = function($task, idLink, isExpanded, instantly) {
		if(typeof isExpanded === "undefined") {
			isExpanded = taskSwitcher.isTaskExpanded($task);
		}
		$task.addClass("removed");
		current.splice(current.indexOf(idLink), 1);
		var promise = Promise.resolve();
		if (isExpanded) {
			promise = taskSwitcher.collapseTaskIfExpanded($task, idLink, taskListId, instantly);
		}
		return promise.then(function() {
			$task.remove();
		});
	};
	
	/**
	 * Async. Removes all given tasks from page.
	 * @returns {Promise}
	 */
	var removeTasks = function(idLinks, instantly) {
		return Promise.all(idLinks.map(function(id) {
			return removeTask(findTask(id), id, undefined, instantly);
		}));
	};
	
	/**
	 * Async. Adds all given tasks to cache and to page.
	 */
	var addTasks = function(idLinks) {
		return cache.makeAvailable(idLinks)
		.then(function() {
			appendTaskHeaders(idLinks);
		});
	};
	
	/**
	 * Async. Adds the fiven task to cache and to page at given position.
	 */
	var addTask = function(idLink, pos) {
		return cache.makeAvailable([idLink])
		.then(function() {
			appendTaskHeaders([idLink]);
			moveTask(idLink, current.length-1, pos);
		});
	};
	
	var updateTaskList = function() {
		// TODO more elegant way to update the cache?
		return cache.updateCache()
		.then(updateTaskListOld);
	};
	
	/**
	 * Async. Get list of currently visible tasks.
	 * @returns {Promise}
	 */
	var updateTaskListOld = function() {
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
		current = visibleIdsOrdered;
	};
	
	var taskListEventHandlers = {
			
		FilterAll : function(event) {
			var newVisibleIds = event.visibleIdsOrdered;
			// remove tasks which became invisible
			var hidden = current.filter(function(id) {
				return newVisibleIds.indexOf(id) < 0;
			});
			return removeTasks(hidden, true)
			.then(function() {
				// add tasks which became visible
				var addedIds = newVisibleIds.diff(current);
				return addTasks(addedIds)
				// resort
				.then(function() {
					resortTaskList(newVisibleIds);
				});
			});
		},
		
		SortAll : function(event) {
			resortTaskList(event.visibleIdsOrdered);
			return Promise.resolve();
		},
		SortSingle : function(event) {
			moveTask(event.movedId, undefined, event.newPos);
			return Promise.resolve();
		},
		
		AddAll : function(event) {
			return addTasks(event.addedIds)
			.then(function() {
				resortTaskList(event.visibleIdsOrdered);
			});
		},
		AddSingle : function(event) {
			return addTask(event.addedId, event.insertedAtPos);
		},
		
		RemoveAll : function(event) {
			return removeTasks(event.removedIds, true);
		},
		RemoveSingle : function(event) {
			var id = event.removedId;
			return removeTask(findTask(id), id, undefined, true);
		}
	};
	
	/**
	 * @param {DataChangeEvent} event
	 * @returns {Promise}
	 */
	var onDataChangeEvent = function(event) {
		var handler;
		switch(event.eventType) {
		case "REMOVED" : handler = onDeleted; break;
		case "EDITED" : handler = onEdited; break;
		default : return Promise.resolve();
		}
		var tasks = [];
		for (let id of event.changedIds) {
			if (current.indexOf(id) >= 0) {
				tasks.push(function() {
					return handler(id, event.source.idLink);
				});
			}
		}
		return runSerial(tasks);
	};
	
	var onDeleted = function(taskId, userId) {
		var $task = findTask(taskId);
		var isExpanded = taskSwitcher.isTaskExpanded($task);
		if (isExpanded && userId !== settings.currentUser.idLink) {
			var $confirm = Dialogs.alert(function() {
				Dialogs.hideDialog($confirm);
			}, "A task you had selected has been deleted by another user!");
		}
		return removeTask($task, taskId, isExpanded);
	};
	
	var onEdited = function(taskId, userId) {
		var $task = findTask(taskId);
		var isExpanded = taskSwitcher.isTaskExpanded($task);
		if (isExpanded && userId !== settings.currentUser.idLink) {
			var $confirm = Dialogs.alert(function() {
				Dialogs.hideDialog($confirm);
			}, "A task you had selected has been edited by another user!");
		}
		var pos = current.indexOf(taskId);
		return removeTask($task, taskId, isExpanded)
		.then(function() {
			return addTask(taskId, pos);
		});
	};
	
	var init = function() {
		taskSwitcher.registerTaskList(taskListId, {
			
			createBody : function($task) {
				var idLink = $task.attr('id');
				var $body = cache.getTaskBody(idLink);
				settings.eventBinders.bindBody(idLink, $task, $body, updateTaskList);
				return $body;
			},
			
			destroyBody : function($body) {
				$body.remove();
			}
		});
		
		settings.eventBinders.bindList(settings.$list, function($task) {
			taskSwitcher.switchTask($task, getIdOfTask($task[0]), taskListId);
		});
		
		cache.registerEventSubscriber(taskListId, onDataChangeEvent);
	};
	init();
	
	return {
		
		/**
		 * Call this to cause this list to get updates from the server and thus sync it.
		 */
		update : function() {
			updateTaskList();
		},
		
		size : function() {
			return current.length;
		}
		
	};
};

export default TaskList;