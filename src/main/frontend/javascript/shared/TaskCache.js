import Ajax from "./ajax";
import HtmlPreProcessor from "./preprocessor";
import { contextUrl } from "./default";
import $ from "../lib/jquery";

/**
 * Holds rendered task data in a cache to avoid unnecessary requests & rendering.
 * @author Jan Mothes
 * 
 * @param url - Url used to get rendered task data for a list of task ids.
 */
var TaskCache = function(url) {
		
	var idToTaskData = {};
	
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
	
	return {
		
		loadTasks : function(idLinks) {
			return Ajax.post(contextUrl + url, { "idLinks[]" : idLinks })
			.then(function (taskRenders) {
				taskRenders.forEach(function(task) {
					preprocess(task);
					idToTaskData[task.idLink] = task;
				});
			});
		},
		
		deleteTask : function(idLink) {
			delete idToTaskData[idLink];
		},
		
		getMissingIds : function(idLinks) {
			var missing = [];
			idLinks.forEach(function(id) {
				if (!idToTaskData.hasOwnProperty(id)) {
					missing.push(id);
				}
			});
			return missing;
		},
		
		getTaskHeader : function(idLink) {
			return idToTaskData[idLink].$header.clone();
		},
		getTaskBody : function(idLink) {
			return idToTaskData[idLink].$body.clone();
		}
	};
};

export default TaskCache;