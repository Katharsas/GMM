/**
 * -------------------- TaskSwitcher -------------------------------------------------------------
 * Provides the two static methods collapse and expand, which load and animate a tasks details.
 * Also provides the switchTask method which will collapse or expand tasks as needed automatically.
 * Depends on TweenLite library.
 */
var TaskSwitcher = function(taskLoader) {	
	this.slideDownTime = 0.5;
    this.slideUpTime = 0.5;
    
	//default is 60
	TweenLite.ticker.fps(30);
    
    var getBody = function($task) {
    	return $task.children(".listElementBody");
    };
    
    var addTaskFileTrees = function($element) {
    	var idLink = $element.attr('id');
    	var url = idLink;
    	$element.find('#assetFilesContainer').fileTree(
    			allFuncs.treePluginOptions("tasks/files/assets/" + url, false),
    			function($file) {
    				tasksVars.selectedTaskFileIsAsset = true;
    				allFuncs.selectTreeElement($file, "selectedTaskFile");
    			});
    	$element.find('#wipFilesContainer').fileTree(
    			allFuncs.treePluginOptions("tasks/files/other/" + url, false),
    			function($file) {
    				tasksVars.selectedTaskFileIsAsset = false;
    				allFuncs.selectTreeElement($file, "selectedTaskFile");
    			});
    };
    
    return {
        /**
         * Plays a slideUp animation and removes task detail DOM from page.
         */
        collapse : function($task) {
            if($task !== undefined) {
            	var $body = getBody($task);
            	TweenLite.to($body, slideUpTime, {height: "0px", onComplete: function() {
            			$task.removeAttr("style");
            	        taskLoader.removeBody($task);
                    }
                });
            }
        },
        
        /**
         * Adds task detail DOM to page and plays a slideDown animation. 
         */
        expand : function($task) {
        	if ($task !== undefined) {
        		taskLoader.insertBody($task);
        		var $body = getBody($task);
        		
        		//TODO move following to css
        		$body.find(".elementComments:blank, .elementDetails:blank").hide();
        		$body.find(".commentForm").hide();
        		$body.find(".elementDelete").hide();
        		//TODO end
        		
        		addTaskFileTrees($task);
        		$task.css("border-width", "2px");
        		$task.css("padding-left", "6px");
        		$task.css("background-color", "#000");
        		$task.css("background-color", "rgba(0, 32, 48, 1)");
        		$body.show();
                TweenLite.from($body, slideDownTime, {height: "0px", onComplete: function() {
                		$body.css("height","");
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
};