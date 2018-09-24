/*jshint loopfunc: true */

import Ajax from "../shared/ajax";
import Dialogs from "../shared/dialogs";
import { switchPinOperation } from "./Task";
import HtmlPreProcessor from "../shared/preprocessor";
import { contextUrl, resortElementsById, runSerial } from "../shared/default";
import { IllegalArgumentException } from "../shared/Errors"
import TaskCache from "./TaskCache";

/** default parameter for required arguments */
const r = function() {
	throw new IllegalArgumentException();
}

/**
 * @author Jan Mothes
 * 
 * 
 * @typedef TaskListSettings
 * @property {JQuery} $list - The container element which holds all task elements as children.
 * @property {string} eventUrl - The url path providing taskList events for synchronization.
 * @property {string} initUrl - Optional: The url path to call to init the list state on the server side.
 * @property {Callback} onUpdateStart - Optional: Executed whenever the tasklist starts updating/changing itself.
 * @property {Callback} onUpdateDone - Optional: Executed whenever the taskList has updated/changed itself.
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
 * Create & initialize a task list.
 * 
 * @param {TaskListSettings} settings - Settings needed to create this taskList.
 * @param {TaskCache} cache - Task data container.
 * @param {TaskSwitcher} taskSwitcher - Contains state about which tasks are expanded/collpased.
 * @param {Object.<string, TaskEventHandler>} eventHandlers - Map of event types to event handlers.
 * 		Extends the existing baseEventHandlers with additional handlers, which will be executed
 * 		after the baseEventHandlers.
 */
const TaskList = function(settings =r, cache =r, taskSwitcher =r, eventHandlers =r) {

	const taskListId = settings.taskListId;
	const taskSelector = ".task:not(.removed)";
	
	/**
	 * Ids of tasks which are currently visible.
	 */
	let current = [];
	
	const getIdOfTask = function(task =r) {return task.id;};
	
	const findTask = function(idLink =r) {
		if (current.indexOf(idLink) < 0) return null;
		else {
			return settings.$list.children(taskSelector + "#" + idLink);
		}
	};
	
	const getHeader = function(idLink =r) {
		const $header = cache.getTaskHeader(idLink);
		settings.eventBinders.bindHeader($header);
		return $header;
	};
	
	/**
	 * Append given tasks to list. Task data for them must exist in cache.
	 * Does not check if task header has already been added to the list.
	 */
	const appendTaskHeaders = function(idLinks =r) {
		for (const id of idLinks) {
			const $header = getHeader(id);
			settings.$list.append($header);
			current.push(id);
			taskSwitcher.expandIfWanted($header, id, taskListId, true);
		}
	};
	
	/**
	 * Move given task from given old position to new position.
	 * @param oldPos - Old position of the task or undefined if unknown.
	 */
	const moveTask = function(idLink =r, oldPos, newPos =r) {
		if (oldPos === undefined) {
			oldPos = current.indexOf(idLink);
		}
		const $task = findTask(idLink);
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
	 * @param {string} idLink
	 * @param {boolean} isExpanded - Wether the task is currently expanded or collapsed.
	 * 		Can be undefined, in which case this function will figure out if it is.
	 * @param {boolean} instantly - Wether an expanded task should collapse instantly or animated.
	 * 		Can be undefined, in which case it defaults to false (animation will play).
	 * @returns {Promise}
	 */
	const removeTask = function($task =r, idLink =r, isExpanded, instantly) {
		if($task === undefined) {
			$task = findTask(idLink);
		}
		if(isExpanded === undefined) {
			isExpanded = taskSwitcher.isTaskExpanded($task);
		}
		$task.addClass("removed");
		current.splice(current.indexOf(idLink), 1);
		let promise = Promise.resolve();
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
	const removeTasks = function(idLinks =r, instantly) {
		return Promise.all(idLinks.map(function(id) {
			return removeTask(findTask(id), id, undefined, instantly);
		}));
	};
	
	/**
	 * Async. Adds all given tasks to cache and to page.
	 */
	const addTasks = function(idLinks =r) {
		if (idLinks == null) throw new IllegalArgumentException();
		return cache.makeAvailable(idLinks)
		.then(function() {
			appendTaskHeaders(idLinks);
		});
	};
	
	/**
	 * Async. Adds the fiven task to cache and to page at given position.
	 */
	const addTask = function(idLink =r, pos =r) {
		return cache.makeAvailable([idLink])
		.then(function() {
			appendTaskHeaders([idLink]);
			moveTask(idLink, current.length-1, pos);
		});
	};
	
	/**
	 * Async. Retrieve and process events.
	 * @returns {Promise}
	 */
	const updateTaskList = function() {
		if (settings.onUpdateStart != null) {
			settings.onUpdateStart();
		}
		return Ajax.get(contextUrl + settings.eventUrl)
		.then(function(taskListEvents) {
			const asyncTasks = [];
			for (const event of taskListEvents) {
				console.debug("TaskList: Received event '" + event.eventName + "'");
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
			if (settings.onUpdateDone != null) {
				settings.onUpdateDone(current.length);
			}
		});
	};
	
	const resortTaskList = function(visibleIdsOrdered) {
		resortElementsById(visibleIdsOrdered, settings.$list, taskSelector, getIdOfTask);
		current = visibleIdsOrdered;
	};
	
	/** The interface between TaskList and EventHandlers for TaskList.
	 *  Allows handlers to call "internal" functions. Poor man's inheritance.
	 */
	const eventHandlerInterface = {
			
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

	/** Prompts warning if tasks are expanded & were deleted by user, then removes tasks.
	 * @param {[string]} changedIds 
	 * @param {[string]} userId 
	 */
	const promptAndRemoveTasks = function(changedIds =r, user =r, edited =r) {
		const tasks = {};
		const selected = [];
		const otherUser = user.idLink !== settings.currentUser.idLink;

		for (const taskId of changedIds) {
			const $task = findTask(taskId);
			const isExpanded = taskSwitcher.isTaskExpanded($task);
			tasks[taskId] = {
				$task : $task,
				isExpanded : isExpanded,
				instantly : !(isExpanded && otherUser)
			};
			if (isExpanded && otherUser) {
				selected.push(taskId);
			}
		}
		let promptClosed;
		if (selected.length > 0) {
			promptClosed = promptSelectedChangedDialog(selected, edited);
		} else {
			promptClosed = Promise.resolve();
		}
		return Promise.all(Object.entries(tasks).map(function([taskId, task]) {
			return promptClosed.then(function() {
				return removeTask(task.$task, taskId, task.isExpanded, task.instantly);
			});
		}));
	}
	
	const baseEventHandlers = {
		
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
			return promptAndRemoveTasks(event.removedIds, event.source, false);
		},
		RemoveSingle : function(event) {
			return promptAndRemoveTasks([event.removedId], event.source, false);
		},

		EditAll : function(event) {
			return promptAndRemoveTasks(event.removedIds, event.source, true)
			.then(function() {
				return addTasks(event.addedIds)
			})
			.then(function() {
				return resortTaskList(event.visibleIdsOrdered);
			});
		},
		EditSingle : function(event) {
			const promise = current.includes(event.editedId) ? 
				promptAndRemoveTasks([event.editedId], event.source, true) : Promise.resolve();

			return promise.then(function() {
				if (event.newPos >= 0) {
					return addTask(event.editedId, event.newPos);
				}
			});
		},
	};

	/**
	 * Async. Informs users about edits/deletes of given tasks by other users.
	 * @param {boolean} edited - true if edited, false if deleted
	 */
	const promptSelectedChangedDialog = function(taskIds =r, edited =r) {
		const actionString = edited ? "edited" : "deleted";
		return new Promise((resolve, reject) => {
			Dialogs.alert(function() {
				resolve();
			}, "One or more tasks you had selected have been " + actionString + " by another user!");
		});
	}
	
	const init = function() {

		const serverInitDone = settings.initUrl == null ?
			Promise.resolve() : Ajax.post(contextUrl + settings.initUrl);

		taskSwitcher.registerTaskList(taskListId, {
			
			createBody : function($task) {
				const idLink = $task.attr('id');
				const $body = cache.getTaskBody(idLink);
				settings.eventBinders.bindBody(idLink, $body);
				HtmlPreProcessor.lazyload($body);
				return $body;
			},
			
			destroyBody : function($body) {
				settings.eventBinders.unbindBody($body);
				$body.remove();
			}
		});
		
		const onswitch = function($task) {
			taskSwitcher.switchTask($task, getIdOfTask($task[0]), taskListId);
		};
		settings.eventBinders.bindList(settings.$list, onswitch, updateTaskList);

		cache.subscribePinnedEvent(function(idLink, isPinned) {
			const $task = findTask(idLink);
			if ($task != null) {
				switchPinOperation($task, isPinned);
			}
		});

		serverInitDone.then(updateTaskList);
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