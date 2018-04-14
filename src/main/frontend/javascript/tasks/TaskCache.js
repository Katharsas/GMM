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

	const currentlyLoadingIds = [];
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
		HtmlPreProcessor.apply(task.$header);
		
		task.$body = $(task.body);
		delete task.body;
		HtmlPreProcessor.apply(task.$body);
		
		task.$body.hide();
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
			console.log("Task cache updated!");
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
	
	/**
	 * @param {string[]} idLinks 
	 */
	const loadTasks = function(idLinks) {
		if (idLinks.length <= 0) {
			return currentlyLoadingPromise;
		}
		currentlyLoadingIds.push(...idLinks);
		currentlyLoadingPromise = currentlyLoadingPromise.then(function() {
			const idLinksMissing = idLinks.slice();
			console.debug("TaskCache: Loading task data for ids: " + idLinks);
			const data = { "idLinks[]" : idLinks };

			return Ajax.post(contextUrl + renderUrl, data)
			.then(function (taskRenders) {
				taskRenders.forEach(function(taskData) {
					preprocess(taskData.render);
					const idLink = taskData.idLink;
					idToTaskData[idLink] = taskData;
					// TODO: is there a better method to remove first equal object from arrays?
					delete idLinksMissing[idLinksMissing.indexOf(idLink)];
					delete currentlyLoadingIds[currentlyLoadingIds.indexOf(idLink)];
					console.debug("TaskCache: Loading done for id: " + idLink);
				});
				for (const idLink of idLinksMissing) {
					// TODO: how to clean up an array with empty slots?
					// TODO: cleanup undefineds in idLinksMissing and currentlyLoading
					if (idLink !== undefined) {
						idToTaskData[idLink] = getDummyTask(idLink);
						delete currentlyLoadingIds[currentlyLoadingIds.indexOf(idLink)];
						console.debug("TaskCache: Loading done for id: " + idLink);
					}
				}
			});
		});
		return currentlyLoadingPromise;
	};

	const getDummyTask = function(idLink) {
		return {
			idLink : idLink,
			$header : $("<div id='" + idLink + "' class='list-element task collapsed' style='padding:5px'>"
				+ "Error: Task with id '" + idLink + "' was not returned from server!</div>")
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
		 * @returns true, false or undefined (is current user is not logged in)
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