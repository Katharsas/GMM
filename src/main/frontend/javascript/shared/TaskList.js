import $ from "../lib/jquery";
import Ajax from "./ajax";
import Dialogs from "./dialogs";
import { contextUrl, resortElementsById } from "./default";
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
			
			releaseBody : function($body) {
			}
		}
	);
	
	settings.eventBinders.bindList(settings.$list, function($task) {
		taskSwitcher.switchTask($task);
	});
	
	var findTask = function(idLink) {
		if (current.indexOf(idLink) < 0) return null;
		else {
			return settings.$list.children(".task#" + idLink);
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
		idLinks.forEach(function(id) {
			var $header = getHeader(id);
			settings.$list.append($header);
		});
	};
	
	/**
	 * Async. Remove task from list as soon as the user changes them or when delete event occurs.
	 * @returns {Promise}
	 */
	var removeTask = function($task, idLink, isExpanded) {
		return (isExpanded ? taskSwitcher.collapseTaskIfExpanded($task) : Promise.resolve())
		.then(function() {
			$task.hide();
			$task.remove();
			current.splice(current.indexOf(idLink), 1);
		});
	};
	
	/**
	 * Async.
	 * @returns {Promise}
	 */
	var markDeprecated = function($task, idLink) {
		if ($task === null) $task = findTask(idLink);
		return removeTask($task, idLink, taskSwitcher.isTaskExpanded($task))
		.then(updateTaskList);
	};
	
	/**
	 * Async. Get list of currently visible tasks.
	 * @returns {Promise}
	 */
	var updateTaskList = function() {
		return Ajax.get(contextUrl + settings.eventUrl)
		.then(function(taskListEvents) {
			for(var event of taskListEvents) {
				return taskListEventHandlers[event.eventName](event);
			}
		})
		.then(function() {
			if (settings.onChange !== null) {
				settings.onChange(current.length);
			}
		});
	};
	
	var getIdOfTask = function(task) {return task.id;};
	var resortTaskList = function(visibleIdsOrdered) {
		resortElementsById(visibleIdsOrdered, settings.$list, ".task", getIdOfTask);
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
				settings.$list.children(".task").each(function() {
					var id = getIdOfTask(this);
					if (newVisibleIds.indexOf(id) < 0) {
						$(this).remove();
					}
				});
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
				settings.$list.children(".task").eq(pos).before($header);
				current.splice(pos, 0, id); 
			});
		},
		
		RemoveAll : function(event) {
			// remove all from page & cache
			// TODO
			return Promise.resolve();
		},
		
		RemoveSingle : function(event) {
			var id = event.removedId;
			// delete from cache
			cache.deleteTask(id);
			// check if task is currently visible
			if(current.indexOf(id) >= 0) {
				var $task = findTask(id);
				// if expanded, show message that the expanded tasks was deleted and switch it
				// TODO make dialogs promisable and wait for ok
				var isExpanded = taskSwitcher.isTaskExpanded($task);
				if (isExpanded) {
					var $confirm = Dialogs.alert(function() {
						Dialogs.hideDialog($confirm);
						removeTask($task, id, isExpanded);
					}, "A task you have selected has been deleted or updated!");
				} else {
					removeTask($task, id, isExpanded);
				}
			}
			return Promise.resolve();
		}
	};

	
	return {
		
		/**
		 * Call this to cause this list to get updates from the server and thus sync it.
		 */
		update : updateTaskList,
		
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