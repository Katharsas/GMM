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
var TaskCache = function(settings) {
	
	var idToTaskData = {};
	
	var subscriberToEventHandler = {};
	
	var currentlyUpdating = false;
	var currentUpdatePromise = undefined;
	
	/**
	 * - converts html strings to dom elements
	 * - converts svg links to svg code
	 * - hides bodies
	 */
	var preprocess = function(task) {
		
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
	 * Also calls callbacks from event subscribers. Returns promise of all processing finished.
	 * @returns {Promise}
	 */
	var updateCache = function() {
		return Ajax.get(contextUrl + settings.eventUrl)
		.then(function(events) {
			var tasks = [];
			for(let event of events) {
				tasks.push(function() {
					return eventHandlers[event.eventType](event)
					.then(function() {
						return Promise.all(Object.keys(subscriberToEventHandler).map(function(id) {
							return subscriberToEventHandler[id](event);
						}));
					});
				});
			}
			return runSerial(tasks);
		});
	};
	
	/**
	 * DataChangeEvent handlers. Always return a Promise, event if not async.
	 */
	var eventHandlers = {
		ADDED : function(event) {
			return Promise.resolve();// too lazy
		},
		REMOVED : function(event) {
			for (let id of event.changedIds) {
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
	var loadTasks = function(idLinks) {
		var idLinksMissing = idLinks.slice();
		var data = { "idLinks[]" : idLinks };
		return Ajax.post(contextUrl + settings.renderUrl, data)
		.then(function (taskRenders) {
			taskRenders.forEach(function(task) {
				preprocess(task);
				idToTaskData[task.idLink] = task;
				delete idLinksMissing[idLinksMissing.indexOf(task.idLink)];
			});
			for (var idLink of idLinksMissing) {
				idToTaskData[idLink] = getDummyTask(idLink);
			}
		});
	};

	var getDummyTask = function(idLink) {
		return {
			idLink : idLink,
			$header : $("<div id='" + idLink + "' class='list-element task collapsed' style='padding:5px'>"
				+ "Error: Task with id '" + idLink + "' was not returned from server!</div>")
		};
	}
	
	return {
		
		/**
		 * @param {callback} onEvent - Has one argument of type DataChangeEvent.
		 */
		registerEventSubscriber : function(subscriberId, onEvent) {
			subscriberToEventHandler[subscriberId] = onEvent;
		},
		
		unregisterEventSubscriber : function(subscriberId) {
			delete subscriberToEventHandler[subscriberId];
		},
		
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
			var missing = [];
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