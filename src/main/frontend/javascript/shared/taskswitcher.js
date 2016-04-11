import TweenLite from "../lib/tweenLite";

/**
 * -------------------- TaskSwitcher -------------------------------------------------------------
 * Provides the two static methods collapse and expand, which load and animate a tasks details.
 * Also provides the switchTask method which will collapse or expand tasks as needed automatically.
 * Depends on TweenLite library.
 * 
 * @author Jan Mothes
 */
export default function(taskListId, taskLoader) {	
	
	var slideDownTime = 0.5;
    var slideUpTime = 0.5;
    
	//default is 60
	TweenLite.ticker.fps(30);
	
	function getBody($task) {
		return $task.children(":last-child");
	}
    
    return {

        /**
         * Plays a slideUp animation and removes task detail DOM from page.
         */
        collapse : function($task) {
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
            	        taskLoader.removeBody($body);
            	        $task.removeClass("collapsing");
                    	$task.addClass("collapsed");
                    }
                });
            }
        },
        
        /**
         * Adds task detail DOM to page and plays a slideDown animation. 
         */
        expand : function($task) {
        	if ($task !== undefined && $task !== null) {
        		
        		if(!$task.hasClass("collapsing")) {
        			taskLoader.insertBody(taskListId, $task);
        		}
        		$task.removeClass("collapsing");
        		$task.removeClass("collapsed");
        		$task.addClass("expanding");
            	
        		var $body = getBody($task);
        		
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
        	        }
        	    });
        	}
        },
        
        /**
         * Called when user clicks on a task header. Adds/removes this task's body an may
         * remove other task's bodies if given queue is full.
         */
        switchTask : function($task, expandedTasksQueue) {
            var $newElement = $task;
            var isAlreadyExpanded = expandedTasksQueue.contains($newElement);
            var $collapse;
            var $expand;
            
            //define which element needs to be expanded/collapsed and add/remove them from expandedTasks queue
            if(isAlreadyExpanded) {
            	$expand = undefined;
            	$collapse = $newElement;
            	expandedTasksQueue.remove($collapse);
            }
            else {
            	$expand = $newElement;
            	$collapse = expandedTasksQueue.add($expand);
            }
            this.collapse($collapse);
            this.expand($expand);
        }
    };
}