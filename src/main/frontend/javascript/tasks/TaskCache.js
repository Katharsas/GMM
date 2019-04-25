/*jshint loopfunc: true, -W080 */

import Ajax from "../shared/ajax";
import HtmlPreProcessor from "../shared/preprocessor";
import DataChangeNotifier from "../shared/DataChangeNotifier";
import { contextUrl, runSerial } from "../shared/default";
import $ from "../lib/jquery";

/**
 * Holds rendered task data in a cache to avoid unnecessary requests & rendering.
 * @author Jan Mothes
 * 
 * @typedef UserId
 * @property {string} idLink
 * @property {string} name
 * 
 * @typedef DataChangeEvent
 * @property {UserId} source
 * @property {string} eventType - enum, possible values: "ADDED", "REMOVED" or "EDITED".
 * @property {string[]} changedIds - ids of the tasks that were changed as specified by eventType.
 * 
 * @param {string} renderUrl - used to get rendered task data for any task ids.
 */
const TaskCache = function(renderUrl) {
	
	const idToTaskData = {};

	let currentlyLoadingIds = [];
	let currentlyLoadingPromise = Promise.resolve();

	const pinnedSubscribers = [];
	
	/**
	 * - converts html strings to dom elements
	 * - converts svg links to svg code
	 * - hides bodies
	 */
	const preprocess = function(task) {
		
		task.$header = $(task.header);
		delete task.header;
		const headerDone = HtmlPreProcessor.apply(task.$header);
		
		task.$body = $(task.body);
		delete task.body;
		const bodyDone = HtmlPreProcessor.apply(task.$body);
		
		task.$body.hide();

		return Promise.all([headerDone, bodyDone]);
	};
	
	/**
	 * Serially processes the events to update the cache.
	 * Returns promise of all processing finished.
	 * @returns {Promise}
	 */
	const updateCache = function(events) {
		const tasks = [];
		for (const event of events) {
			const eventHandler = eventHandlers[event.eventType];
			if (eventHandler !== undefined) {
				tasks.push(function() {
					return eventHandler(event);
				});
			}
		}
		return runSerial(tasks).then(function() {
			console.debug("TaskCache: Cache updated.");
		});
	};
	
	/**
	 * DataChangeEvent handlers. Always return a Promise, event if not async.
	 */
	const eventHandlers = {
		REMOVED : function(event) {
			for (const id of event.changedIds) {
				delete idToTaskData[id];
			}
			return Promise.resolve();
		},
		EDITED : function(event) {
			return loadTasks(event.changedIds);
		}
	};

	const removeFirst = function(array, element) {
		const index = array.indexOf(element);
		if (index !== -1) {
			delete array[index];
		}
		return index !== -1;
	}
	
	/**
	 * @param {string[]} idLinks 
	 */
	const loadTasks = function(idLinks) {
		if (idLinks.length <= 0) {
			return currentlyLoadingPromise
			.then(() => []);
		}
		currentlyLoadingIds.push(...idLinks);
		currentlyLoadingPromise = currentlyLoadingPromise.then(function() {
			let idLinksMissing = idLinks.slice();
			console.debug("TaskCache: Loading task data for ids: " + idLinks);
			const data = { "idLinks[]" : idLinks };

			return Ajax.post(contextUrl + renderUrl, data)
			.then(function (taskRenders) {
				const preprocessPromises = [];
				taskRenders.forEach(function(taskData) {
					const preprocessDone = preprocess(taskData.render)
					.then(function() {
						const idLink = taskData.idLink;
						idToTaskData[idLink] = taskData;
						removeFirst(idLinksMissing, idLink);
						removeFirst(currentlyLoadingIds, idLink);
						console.debug("TaskCache: Loading done for id: " + idLink);
					});
					preprocessPromises.push(preprocessDone);
				});
				return Promise.all(preprocessPromises);
			})
			.then(function() {
				for (const idLink of idLinksMissing) {
					if (idLink !== undefined) {
						idToTaskData[idLink] = getDummyTask(idLink);
						removeFirst(currentlyLoadingIds, idLink);
						console.debug("TaskCache: Loading done for id: " + idLink);
					}
				}
				currentlyLoadingIds = currentlyLoadingIds.filter(__ => true);
				idLinksMissing = idLinksMissing.filter(__ => true);
				return idLinksMissing;
			});
		});
		return currentlyLoadingPromise;
	};

	const getDummyTask = function(idLink) {
		return {
			idLink : idLink,
			isPinned : false,
			render : {
				$header : $("<div id='" + idLink + "' class='list-element task collapsed' style='padding:5px'>"
				+ "Error: Task with id '" + idLink + "' was not returned from server!</div>"),
				$body : $("<div class='task-body' css='display:none;'></div>")
			}
		};
	}

	const logState = function(logFunction) {
		logFunction("Cached Ids:");
		for (const id of Object.entries(idToTaskData)) {
			logFunction(id);
		}
	}

	const checkThrowTaskNotFound = function(idLink) {
		if (!(idLink in idToTaskData)) {
			throw new Error("Could not find data for task '" + idLink + "' in cache!");
		}
	}

	DataChangeNotifier.registerSubscriber("TaskCache", function(events) {
		return updateCache(events);
	});
	
	return {
		
		/**
		 * Before getting a task header or task body, the task must be loaded into the cache.
		 * This function ensures that all given tasks are present in the cache. Since calling
		 * this function may cause a request, try to call it seldom.
		 * @returns {string[]} idLinks that could not be resolved by the server
		 */
		makeAvailable : function(idLinks) {
			const toLoad = [];
			idLinks.forEach(function(id) {
				const isMissing = !idToTaskData.hasOwnProperty(id);
				const isLoading = currentlyLoadingIds.includes(id);
				if (isMissing && !isLoading) {
					toLoad.push(id);
				}
			});
			return loadTasks(toLoad);
		},
		
		/**
		 * Will fail if the task is not in the cache.
		 * @see function makeAvailable
		 */
		getTaskHeader : function(idLink) {
			checkThrowTaskNotFound(idLink);
			return idToTaskData[idLink].render.$header.clone();
		},
		
		/**
		 * Will fail if the task is not in the cache.
		 * @see function makeAvailable
		 */
		getTaskBody : function(idLink) {
			checkThrowTaskNotFound(idLink);
			return idToTaskData[idLink].render.$body.clone();
		},


		/** 
		 * All of this pinned stuff is a hack, because it is not clear how to do this properly.
		 * (Would require backend architecture changes).
		 * @returns true, false or undefined (if current user is not logged in)
		 */
		isPinned : function(idLink) {
			checkThrowTaskNotFound(idLink);
			return idToTaskData[idLink].isPinned;
		},

		/**
		 * Subscribe to get info about pinning/unpinning of tasks.
		 * @param {function} callback - Parameters: idLink : string, isPinned : bool
		 */
		subscribePinnedEvent(callback) {
			pinnedSubscribers.push(callback);
		},

		/**
		 * @param {*} isPinned - true, false or undefined (if current user is not logged in)
		 */
		triggerIsPinned(idLink, isPinned) {
			checkThrowTaskNotFound(idLink);
			idToTaskData[idLink].isPinned = isPinned;
			for (const callback of pinnedSubscribers) {
				callback(idLink, isPinned);
			}
		}
	};
};

export default TaskCache;