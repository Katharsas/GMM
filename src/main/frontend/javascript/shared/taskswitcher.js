import TweenLite from "../lib/tweenLite";
import Queue from "./queue";

/**
 * -------------------- TaskSwitcher -------------------------------------------------------------
 * Provides the two static methods collapse and expand, which load and animate a tasks details.
 * Also provides the switchTask method which will collapse or expand tasks as needed automatically.
 * Depends on TweenLite library.
 * 
 * @author Jan Mothes
 * 
 * @param {Object} taskBodyCallbacks - Contains two callbacks:
 * 		createBody($task) : $body - Returns a body ready top be inserted into dom.
 * 		releaseBody($body) : void - Will be called before removing body from dom.
 */
export default function(taskBodyCallbacks) {
	
	var expanded = new Queue(3, function($task1, $task2) {
		return $task1[0] === $task2[0];
	});
	
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
    var collapse = function($task) {
    	return new Promise(function(resolve, reject) {
	        if($task !== undefined && $task!== null) {
	        	$task.removeClass("expanding");
	        	$task.removeClass("expanded");
	        	$task.addClass("collapsing");
	        	
	        	var $body = getBody($task);
	        	var tween = TweenLite.to($body, slideUpTime, {height:0,
	        		onUpdate: function() {
	        			if (!$task.hasClass("collapsing")) {
	        				tween.kill();
	        			}
	        		},
	        		onComplete: function() {
	        			$body.hide();
	        			taskBodyCallbacks.releaseBody($body);
	        			$body.remove();
	        	        $task.removeClass("collapsing");
	                	$task.addClass("collapsed");
	                	resolve();
	                }
	            });
	        } else {resolve();}
    	});
    };
    
    /**
     * Adds task detail DOM to page and plays a slideDown animation.
     * @return {Promise} - Resolved when all animations have completed.
     */
    var expand = function($task) {
    	return new Promise(function(resolve, reject) {
    		if ($task !== undefined && $task !== null) {
        		
        		var $body;
        		if(!$task.hasClass("collapsing")) {
        			$body = taskBodyCallbacks.createBody($task);
        			$task.append($body);
        		} else {
        			$body = getBody($task);
        		}
        		$task.removeClass("collapsing");
        		$task.removeClass("collapsed");
        		$task.addClass("expanding");
        		
        		$body.show();
        		$body.css("height","");
                var tween = TweenLite.from($body, slideDownTime, {height:0,
                	onUpdate: function() {
                		if (!$task.hasClass("expanding")) {
            				tween.kill();
            			}
                	},
                	onComplete: function() {
                		$body.css("height","");
                    	$task.removeClass("expanding");
                		$task.addClass("expanded");
                		resolve();
        	        }
        	    });
        	} else {resolve();}
    	});
    };
    
    return {
        
        /**
         * Called when user clicks on a task header. Adds/removes this task's body an may
         * remove other task's bodies if given queue is full.
         * @return {Promise} - Resolved when all animations have completed.
         */
        switchTask : function($task) {
            var $newElement = $task;
            var isAlreadyExpanded = expanded.contains($newElement);
            var $collapse;
            var $expand;
            
            //define which element needs to be expanded/collapsed and add/remove them from expandedTasks queue
            if(isAlreadyExpanded) {
            	$expand = undefined;
            	$collapse = $newElement;
            	expanded.remove($collapse);
            }
            else {
            	$expand = $newElement;
            	$collapse = expanded.add($expand);
            }
            return Promise.all([
                collapse($collapse),
                expand($expand)
            ]);
           
        },
        
        /**
         * @return True if task is currently expanded or expanding.
         */
        isTaskExpanded : function($task) {
        	return expanded.contains($task);
        },
        
        /**
         * Collapses the task if it is expanded.
         * @return {Promise} - Resolved when all animations have completed.
         */
        collapseTaskIfExpanded : function($task) {
        	 if(this.isTaskExpanded($task)) {
        		 expanded.remove($task);
        		 return collapse($task);
        	 } else {
        		 return Promise.resolve();
        	 }
        }
    };
}