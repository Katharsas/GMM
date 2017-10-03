/*jshint loopfunc: true */

import Ajax from "../shared/ajax";
import Dialogs from "../shared/dialogs";
import lozad from 'lozad';
import { contextUrl, resortElementsById, runSerial } from "../shared/default";

/**
 * @author Jan Mothes
 * 
 * 
 * @typedef TaskListSettings
 * @property {JQuery} $list - The container element which holds all task elements as children.
 * @property {string} eventUrl - The url path providing taskList events for synchronization.
 * @property {Callback} onChange - Executed whenever the taskList has changed. Can be null.
 * 		Count of current tasks will be passed as parameter.
 * @property {TaskEventBindings} eventBinders - Contains all functions for event binding.
 * @property {UserId} currentUser - Current user or null if user is not logged in.
 * 
 * 
 * @callback TaskEventHandler
 * @param {TaskListEvent} event - The event information depending on event type, see Java class.
 * @param {Callback[]} funcs - Internal functions of TaskList are exposed here so they can be used
 * 		by the event handler.
 */

/**
 * @param {TaskListSettings} settings - Settings needed to create this taskList.
 * @param {TaskCache} cache - Task data container.
 * @param {Object.<string, TaskEventHandler>} eventHandlers - Map of event types to event handlers.
 * 		Extends the existing baseEventHandlers with additional handlers, which will be executed
 * 		after the baseEventHandlers.
 */
var TaskList = function(settings, cache, taskSwitcher, eventHandlers) {

	var lozadObserver = lozad();

	var taskListId = settings.taskListId;
	var taskSelector = ".task:not(.removed)";
	
	/**
	 * Ids of tasks which are currently visible.
	 */
	var current = [];
	
	var getIdOfTask = function(task) {return task.id;};
	
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
	 * Append given tasks to list. Task data for them must exist in cache.
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
		if (oldPos === undefined) {
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
	 * @param {JQuery} $task - Task element inside the list, can be undefined if unknown.
	 * @param {bool} isExpanded - Wether the task is currently expanded or collapsed.
	 * 		Can be undefined, in which case this function will figure out if it is.
	 * @param {bool} instantly - Wether an expanded task should collapse instantly or animated.
	 * 		Can be undefined, in which case it defaults to false (animation will play).
	 * @returns {Promise}
	 */
	var removeTask = function($task, idLink, isExpanded, instantly) {
		if($task === undefined) {
			$task = findTask(idLink);
		}
		if(isExpanded === undefined) {
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
			var asyncTasks = [];
			for(let event of taskListEvents) {
				if (event.eventName in baseEventHandlers) {
					asyncTasks.push(function() {
						return baseEventHandlers[event.eventName](event);
					});
				}
				if (event.eventName in eventHandlers) {
					asyncTasks.push(function() {
						return eventHandlers[event.eventName](event, eventHandlerInterface);
					});
				}
			}
			return runSerial(asyncTasks);
		})
		.then(function() {
			if (settings.onChange !== null) {
				settings.onChange(current.length);
			}
		});
	};
	
	var resortTaskList = function(visibleIdsOrdered) {
		resortElementsById(visibleIdsOrdered, settings.$list, taskSelector, getIdOfTask);
		current = visibleIdsOrdered;
	};
	
	var eventHandlerInterface = {
			
		getCurrent : function() {
			return current;
		},
		
		addTasks : addTasks,
		addTask : addTask,
		
		removeTasks : removeTasks,
		removeTask : removeTask,
		
		resortTasks : resortTaskList,
		moveTask : moveTask,
	};
	
	var baseEventHandlers = {
		
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
			return removeTask(undefined, id, undefined, true);
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
		var asyncTasks = [];
		for (let id of event.changedIds) {
			if (current.indexOf(id) >= 0) {
				asyncTasks.push(function() {
					return handler(id, event.source.idLink);
				});
			}
		}
		return runSerial(asyncTasks);
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
		return removeTask($task, taskId, isExpanded, true)
		.then(function() {
			return addTask(taskId, pos);
		});
	};
	
	var init = function() {

		taskSwitcher.registerTaskList(taskListId, {
			
			createBody : function($task) {
				var idLink = $task.attr('id');
				var $body = cache.getTaskBody(idLink);
				settings.eventBinders.bindBody(idLink, $task, $body);
				return $body;
			},
			
			destroyBody : function($body) {
				$body.remove();
			}
		});
		
		var onswitch = function($task) {
			taskSwitcher.switchTask($task, getIdOfTask($task[0]), taskListId);
			lozadObserver.observe();
		};
		settings.eventBinders.bindList(settings.$list, onswitch, updateTaskList);
		
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
		},
		
		contains : function(idLink) {
			return current.indexOf(idLink) >= 0;
		},
		
	};
};

export default TaskList;