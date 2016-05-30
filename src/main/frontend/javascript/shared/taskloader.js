import $ from "../lib/jquery";
import Ajax from "./ajax";
import HtmlPreProcessor from "./preprocessor";
import { contextUrl, resortElementsById } from "./default";
import Errors from "./Errors";

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
		return updateVisibleAndCache(taskListId);
	};
	
	/**
	 * Asynchronous!
	 * Update tasks from server for all task lists.
	 * @return - Promise
	 */
//	var update = function(taskListId, idLinks) {
//		return $.when().then(function(){
//			return loadIntoCache(taskListId, idLinks);
//		}).then(function() {
//			Object.keys(taskListMap).forEach(function(taskListId) {
//				reinsertHeaders(taskListId);
//			});
//		});
//	};
	
	/**
	 * Remove tasks for all task lists.
	 */
//	var remove = function(idLinks) {
//		//delete from cache
//		idLinks.forEach(function(id) {
//			delete idToTaskData[id];
//		});
//		//delete from current & from list itself
//		Object.keys(taskListMap).forEach(function(taskListId) {
//			var taskList = taskListMap[taskListId];
//			var $tasks = taskList.$list.children(".task");
//			idLinks.forEach(function(id) {
//				taskList.current.forEach(function(currentId, index) {
//					if(id === currentId) taskList.current.splice(index, 1);
//				});
//				$tasks.remove("#"+id);
//			});
//		});
//	};
	
	/**
	 * Clears task list, then reinsert headers from cache into task list.
	 * ALL tasks are replaced to update order changes correctly.
	 */
//	var reinsertHeaders = function(taskListId) {
//		var taskList = taskListMap[taskListId];
//		taskList.$list.children(".task").remove();
//		taskList.current.forEach(function(id) {
//			var $header = idToTaskData[id].$header.clone(true, true);
//			taskList.eventBinders.bindHeader($header);
//			taskList.$list.append($header);
//		});
//	};
	
	/**
	 * Task data for given idLinks must exist.
	 * Does not check if task header has already been added to the list.
	 */
	var appendTaskHeaders = function(taskListId, idLinks) {
		var taskList = taskListMap[taskListId];
		idLinks.forEach(function(id) {
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
	var updateVisibleAndCache = (function() {
		return function(taskListId) {
			var taskList = taskListMap[taskListId];
			return Ajax.get(contextUrl + taskList.url + "/taskListEvents")
				.then(function(taskListEvents) {
					for(var event of taskListEvents) {
						return onTaskListEvent(event, taskListId);
					}
				});
		};
		
		// TODO a page can contain multiple tasks with same id (multiple lists)
		// TODO OOP: instead of passing tasklist everywhere, create TaskList class wich owns the methods.
		
		// TODO certain events effect all task lists. If a task gets deleted on list A,
		// it will receive events for listA as an answer, but updates should be triggered for
		// for all other lists => call sync for all lists
		
		// TODO Bug: When element is selected and then CreateSingle event applied,
		// the oriignaly selected element cannot be unselected (bugged)
		
		/**
		 * @callback getIdFromTask extends resortElementsById.getIdOfElement
		 * @param {Element} task
		 */
		function getIdOfTask(task) {
			return task.id;
		}

		function resortTaskList(taskListId, visibleIdsOrdered) {
			var $list = taskListMap[taskListId].$list;
			resortElementsById(visibleIdsOrdered, $list, ".task", getIdOfTask);
		}
		
		
		function getNotYetChachedIds(ids) {
			var missing = [];
			ids.forEach(function(id) {
				if (!idToTaskData.hasOwnProperty(id)) {
					missing.push(id);
				}
			});
			return missing;
		}
		
		function onTaskListEvent(event, taskListId) {
			var taskList = taskListMap[taskListId];
			switch(event.eventName) {
			
			case "FilterAll":
				var newVisibleIds = event.visibleIdsOrdered;
				// load missing into cache
				var missing = getNotYetChachedIds(newVisibleIds);
				return loadIntoCache(taskListId, missing)
				.then(function() {
					// add not visible from cache to page
					var addedIds = newVisibleIds.diff(taskList.current);
					appendTaskHeaders(taskListId, addedIds);
					// remove hidden from page
					taskList.$list.children(".task").each(function() {
						var id = getIdOfTask(this);
						if (newVisibleIds.indexOf(id) < 0) {
							$(this).remove();
						}
					});
					// resort page elements by visible
					resortTaskList(taskListId, newVisibleIds);
					taskListMap[taskListId].current = newVisibleIds;
				});

			case "SortAll":
				// resort page elements by visible
				resortTaskList(taskListId, event.visibleIdsOrdered);
				taskListMap[taskListId].current = event.visibleIdsOrdered;
				return $.when();
				
			case "CreateAll":
				// add createIds into cache and to page (need function)
				return loadIntoCache(taskListId, event.createdIds)
				.then(function() {
					appendTaskHeaders(taskListId, event.createdIds);
				})
				// resort page elements by visible and set current
				.then(function() {
					resortTaskList(taskListId, event.visibleIdsOrdered);
					taskListMap[taskListId].current = event.visibleIdsOrdered;
				});
				
			case "CreateSingle":
				// add id html into cache
				return loadIntoCache(taskListId, [event.createdId])
				// add html to page at given pos
				.then(function() {
					var $header = idToTaskData[event.createdId].$header.clone(true, true);
					taskList.eventBinders.bindHeader($header);
					var pos = event.insertedAtPos;
					taskList.$list.children(".task").eq(pos).before($header);
					taskListMap[taskListId].current.splice(pos, 0, event.createdId); 
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
			//update(taskListId, [idLink]);
			// TODO: sending an edit should return taskEvents to save requests
			updateVisibleAndCache(taskListId);
		},
		
		removeTask : function(idLink) {
			//remove([idLink]);
			// TODO: sending a delete should return taskEvents to save requests
			// TODO for all taskListIds: updateVisibleAndCache(taskListId);
		},
		
		removeTasks : function(idLinks) {
			//remove(idLinks);
			// TODO: sending a delete should return taskEvents to save requests
			// TODO for all taskListIds: updateVisibleAndCache(taskListId);
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
