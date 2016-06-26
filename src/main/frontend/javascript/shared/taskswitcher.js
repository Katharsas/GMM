import TweenLite from "../lib/tweenLite";
import Queue from "./queue";
import Errors from "./Errors";

/**
 * -------------------- TaskSwitcher -------------------------------------------------------------
 * Provides the two static methods collapse and expand, which load and animate a tasks details.
 * Also provides the switchTask method which will collapse or expand tasks as needed automatically.
 * Depends on TweenLite library.
 * 
 * @author Jan Mothes
 * 
 * @type {Object} TaskBodyCallbacks
 * @property {function} createBody($task) : $body - Returns a body ready to be inserted into dom.
 * @property {function} destroyBody($body) : void - Removes body from dom, may perform cleanup.
 */
export default function() {
	
	// queue
	var expanded = new Queue(3, function($task1, $task2) {
		return $task1[0] === $task2[0];
	});
	var wantedExpandedIds = new Queue(10, function(taskId1, taskId2) {
		return taskId1 === taskId2;
	});
	
	// {string} taskListId -> {TaskBodyCallbacks}
	var listIdToCallbacks = {};
	
	// animation settings
	
	var slideDownTime = 0.5;
	var slideUpTime = 0.5;
//    var slideDownTime = 2;
//    var slideUpTime = 2;
    
	//default is 60
	TweenLite.ticker.fps(30);
	
	function getBody($task) {
		return $task.children(":last-child");
	}
	
	  /**
     * Plays a slideUp animation and removes task detail DOM from page.
     * @return {Promise} - Resolved when all animations have completed.
     */
    var collapse = function($task, taskId, taskListId, instant) {
    	if(typeof instant === "undefined") {
    		instant = false;
    	}
    	return new Promise(function(resolve, reject) {
        	$task.removeClass("expanding");
        	$task.removeClass("expanded");
        	$task.addClass("collapsing");
        	
        	var $body = getBody($task);
        	var onComplete = function() {
        		listIdToCallbacks[taskListId].destroyBody($body);
    	        $task.removeClass("collapsing");
            	$task.addClass("collapsed");
            	resolve();
        	};
        	if (instant) {
        		onComplete();
        	} else {
        		var tween = TweenLite.to($body, slideUpTime, {height:0,
            		onUpdate: function() {
            			if (!$task.hasClass("collapsing")) {
            				tween.kill();
            			}
            		},
            		onComplete: onComplete
                });
        	}
    	});
    };
    
    /**
     * Adds task detail DOM to page and plays a slideDown animation.
     * @return {Promise} - Resolved when all animations have completed.
     */
    var expand = function($task, taskId, taskListId, instant) {
    	if(typeof instant === "undefined") {
    		instant = false;
    	}
    	return new Promise(function(resolve, reject) {
    		
    		var $body;
    		if(!$task.hasClass("collapsing")) {
    			$body = listIdToCallbacks[taskListId].createBody($task);
    			$task.append($body);
    		} else {
    			$body = getBody($task);
    		}
    		$task.removeClass("collapsing");
    		$task.removeClass("collapsed");
    		$task.addClass("expanding");
    		
    		$body.show();
    		$body.css("height","");
    		var onComplete = function() {
    			$body.css("height","");
            	$task.removeClass("expanding");
        		$task.addClass("expanded");
        		resolve();
    		};
    		if (instant) {
        		onComplete();
	        } else {
	            var tween = TweenLite.from($body, slideDownTime, {height:0,
	            	onUpdate: function() {
	            		if (!$task.hasClass("expanding")) {
	        				tween.kill();
	        			}
	            	},
	            	onComplete: onComplete
	    	    });
        	}
    	});
    };
    
    return {
    	
    	/**
    	 * Allow tasks of the given taskListId to expand/collapse.
    	 * @param {TaskBodyCallbacks} taskBodyCallbacks - Callbacks used to create/destroy bodies
    	 * 		of tasks of the given taskList.
    	 */
    	registerTaskList : function(taskListId, taskBodyCallbacks) {
    		listIdToCallbacks[taskListId] = taskBodyCallbacks;
    	},
    	
    	unregisterTaskList : function(taskListId) {
    		if(!(taskListId in listIdToCallbacks)) {
    			throw new Errors.IllegalArgumentError("Cannot unregister taskList that is not registered!");
    		}
    		delete listIdToCallbacks[taskListId];
    	},
    	
    	/**
         * @return True if task is currently expanded or expanding.
         */
        isTaskExpanded : function($task) {
        	return expanded.contains($task);
        },
    	
        /**
         * Called when user clicks on a task header. Adds/removes this task's body an may
         * remove other task's bodies if given queue is full.
         * @param {string} taskListId - Id of the task list whose event binders will be called.
         * @return {Promise} - Resolved when all animations have completed.
         */
        switchTask : function($task, taskId, taskListId) {
        	if(!(taskListId in listIdToCallbacks)) {
    			throw new Errors.IllegalArgumentError("Given taskListId is not registered / invalid!");
    		}
            if(this.isTaskExpanded($task)) {
            	// collape
            	expanded.remove($task);
            	return collapse($task, taskId, taskListId);
            } else {
            	// expand: if queue was full, collape element that got removed from queue
            	var $oldest = expanded.add($task);
            	if($oldest !== null) {
            		 return Promise.all([
						collapse($oldest, taskId, taskListId),
						expand($task, taskId, taskListId)
	                 ]);
            	} else {
            		return expand($task, taskId, taskListId);
            	}
            }
        },
        
        /**
         * Collapses the task if it is expanded.
         * @see switchTask for parameter description
         */
        collapseTaskIfExpanded : function($task, taskId, taskListId, instant) {
	    	if(!(taskListId in listIdToCallbacks)) {
	    		throw new Errors.IllegalArgumentError("Given taskListId is not registered / invalid!");
			}
			if(this.isTaskExpanded($task)) {
				expanded.remove($task);
				wantedExpandedIds.add(taskId);
				return collapse($task, taskId, taskListId, instant);
			} else {
				 return Promise.resolve();
			}
        },
        
        expandIfWanted : function($task, taskId, taskListId, instant) {
        	if(wantedExpandedIds.contains(taskId)) {
        		wantedExpandedIds.remove(taskId);
        		if(!this.isTaskExpanded($task) && !expanded.isFull()) {
        			expanded.add($task);
        			return expand($task, taskId, taskListId, instant);
            	}
        	}
        	return Promise.resolve();
        }
    };
}
