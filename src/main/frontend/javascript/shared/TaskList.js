import $ from "../lib/jquery";
import Ajax from "./ajax";
import { contextUrl, resortElementsById } from "./default";
import Queue from "./queue";
import TaskSwitcher from "./taskswitcher";
import Errors from "./Errors";

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
	var taskSwitcher = TaskSwitcher(
		new Queue(3, function($task1, $task2) {
			return $task1[0] === $task2[0];
		}),
		{
			createBody : function($task) {
				var idLink = $task.attr('id');
				var $body = cache.getTaskBody(idLink);
				settings.eventBinders.bindBody(idLink, $task, $body, updateTaskList);
				return $body;
			},
			releaseBody : function($body) {
			}
		}
	);
	
	settings.eventBinders.bindList(settings.$list, function($task) {
		taskSwitcher.switchTask($task);
	});
	
	
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
	 * Asynchronous! Get list of currently visible tasks.
	 * @return - Promise
	 */
	var updateTaskList = function() {
		return Ajax.get(contextUrl + settings.eventUrl)
		.then(function(taskListEvents) {
			for(var event of taskListEvents) {
				return onTaskListEvent(event);
			}
		})
		.then(function() {
			if (settings.onChange !== null) {
				settings.onChange(current.length);
			}
		});
	};
	
	/**
	 * Asynchronous! React to taskListEvent from server.
	 * @return Promise
	 */
	var onTaskListEvent = function(event) {
		
		/**
		 * @callback getIdFromTask extends resortElementsById.getIdOfElement
		 * @param {Element} task
		 */
		function getIdOfTask(task) {
			return task.id;
		}
		
		function resortTaskList(visibleIdsOrdered) {
			resortElementsById(visibleIdsOrdered, settings.$list, ".task", getIdOfTask);
		}
		
		switch(event.eventName) {
		
		case "FilterAll":
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

		case "SortAll":
			// resort page elements by visible
			resortTaskList(event.visibleIdsOrdered);
			current = event.visibleIdsOrdered;
			return $.when();
			
		case "CreateAll":
			// add createIds into cache and to page (need function)
			return cache.loadTasks(event.createdIds)
			.then(function() {
				appendTaskHeaders(event.createdIds);
				// resort page elements by visible and set current
				resortTaskList(event.visibleIdsOrdered);
				current = event.visibleIdsOrdered;
			});
			
		case "CreateSingle":
			// add id html into cache
			return cache.loadTasks([event.createdId])
			// add html to page at given pos
			.then(function() {
				var $header = getHeader(event.createdId);
				var pos = event.insertedAtPos;
				settings.$list.children(".task").eq(pos).before($header);
				current.splice(pos, 0, event.createdId); 
			});
			
		case "RemoveAll":
			// remove from page & cache
			// TODO
			break;
			
		case "RemoveSingle":
			// remove all from page & cache
			// TODO
			break;
			
		default:
			throw new Errors.IllegalArgumentError("Illegal TaskListEvent type: "+event.eventName);
		}
	};
	

	
	return {
		
		/**
		 * Call this to cause this list to get updates from the server and thus sync it.
		 */
		update : updateTaskList,
		
		size : function() {
			return current.length;
		}
		
	};
};

export default TaskList;