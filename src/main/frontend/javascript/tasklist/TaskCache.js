/*jshint loopfunc: true, -W080 */

import Ajax from "../shared/ajax";
import HtmlPreProcessor from "../shared/preprocessor";
import { contextUrl, runSerial } from "../shared/default";
import $ from "../lib/jquery";

/**
 * Holds rendered task data in a cache to avoid unnecessary requests & rendering.
 * @author Jan Mothes
 * 
 * @typedef UserId
 * @property {String} idLink
 * @property {String} name
 * 
 * @typedef DataChangeEvent
 * @property {UserId} source
 * @property {string} eventType - enum, possible values: "ADDED", "REMOVED" or "EDITED".
 * @property {string[]} changedIds - ids of the tasks that were changed as specified by eventType.
 * 
 * @typedef TaskCacheSettings
 * @property {String} renderUrl - used to get rendered task data for any task ids.
 * @property {String} eventUrl - used to get events about task data changes.
 * 
 * 
 * @param {TaskCacheSettings} settings
 */
const TaskCache = function(settings) {
	
	const idToTaskData = {};
	
	const subscriberToEventHandler = {};
	
	let currentlyUpdating = false;
	let currentUpdatePromise = undefined;
	
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
	 * Retrieves all pending DataChangeEvents from the server and serially processes them.
	 * Returns promise of all processing finished.
	 * @returns {Promise}
	 */
	const updateCache = function() {
		return Ajax.get(contextUrl + settings.eventUrl)
		.then(function(events) {
			const tasks = [];
			for (const event of events) {
				const eventHandler = eventHandlers[event.eventType];
				if (eventHandler !== undefined) {
					tasks.push(function() {
						return eventHandler(event);
					});
				}
			}
			return runSerial(tasks);
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
		const idLinksMissing = idLinks.slice();
		const data = { "idLinks[]" : idLinks };
		return Ajax.post(contextUrl + settings.renderUrl, data)
		.then(function (taskRenders) {
			taskRenders.forEach(function(task) {
				preprocess(task);
				idToTaskData[task.idLink] = task;
				delete idLinksMissing[idLinksMissing.indexOf(task.idLink)];
			});
			for (const idLink of idLinksMissing) {
				idToTaskData[idLink] = getDummyTask(idLink);
			}
		});
	};

	const getDummyTask = function(idLink) {
		return {
			idLink : idLink,
			$header : $("<div id='" + idLink + "' class='list-element task collapsed' style='padding:5px'>"
				+ "Error: Task with id '" + idLink + "' was not returned from server!</div>")
		};
	}
	
	return {
		
		updateCache : function() {
			if (!currentlyUpdating) {
				currentlyUpdating = true;
				currentUpdatePromise = updateCache()
				.then(function() {
					currentlyUpdating = false;
					currentUpdatePromise = undefined;
				});
			} else {
				// Do not update if there is an update currently running.
				// Instead just make the caller wait until its finished.
				// Since the first call was probably chained to before a tasklist update
				// the tasklist update is probably still running and cache should not interfere.
				// TODO: synchronize/lock tasklist methods so we CAN interfere without breaking stuff.
			}
			return currentUpdatePromise;
		},
		
		/**
		 * Before getting a task header or task body, the task must be loaded into the cache.
		 * This function ensures that all given tasks are present in the cache. Since calling
		 * this function may cause a request, try to call it seldom.
		 */
		makeAvailable : function(idLinks) {
			const missing = [];
			idLinks.forEach(function(id) {
				if (!idToTaskData.hasOwnProperty(id)) {
					missing.push(id);
				}
			});
			if (missing.length > 0) {
				return loadTasks(missing);
			} else {
				return Promise.resolve();
			}
		},
		
		/**
		 * Will fail if the task is not in the cache.
		 * @see function makeAvailable
		 */
		getTaskHeader : function(idLink) {
			if (!(idLink in idToTaskData)) {
				throw new Error("Could not find data for task '" + idLink + "' in cache!");
			}
			return idToTaskData[idLink].$header.clone();
		},
		
		/**
		 * Will fail if the task is not in the cache.
		 * @see function makeAvailable
		 */
		getTaskBody : function(idLink) {
			if (!(idLink in idToTaskData)) {
				throw new Error("Could not find data for task '" + idLink + "' in cache!");
			}
			return idToTaskData[idLink].$body.clone();
		}
	};
};

export default TaskCache;