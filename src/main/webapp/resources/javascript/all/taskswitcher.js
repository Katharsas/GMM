/**
 * -------------------- TaskSwitcher -------------------------------------------------------------
 * Provides the two static methods collapse and expand, which load and animate a tasks details.
 * Also provides the switchTask method which will collapse or expand tasks as needed automatically.
 * Depends on TweenLite library.
 * 
 * @author Jan Mothes
 */
var TaskSwitcher = function(taskLoader) {	
	this.slideDownTime = 0.5;
    this.slideUpTime = 0.5;
    
	//default is 60
	TweenLite.ticker.fps(30);
    
    var addTaskFileTrees = function($element) {
    	var idLink = $element.attr('id');
    	var url = idLink;
    	$element.find('#assetFilesContainer').fileTree(
    			allFuncs.treePluginOptions(contextUrl + "/tasks/files/assets/" + url, false),
    			function($file) {
    				tasksVars.selectedTaskFileIsAsset = true;
    				allFuncs.selectTreeElement($file, "selectedTaskFile");
    			});
    	$element.find('#wipFilesContainer').fileTree(
    			allFuncs.treePluginOptions(contextUrl + "/tasks/files/other/" + url, false),
    			function($file) {
    				tasksVars.selectedTaskFileIsAsset = false;
    				allFuncs.selectTreeElement($file, "selectedTaskFile");
    			});
    };
    
//    function createAnimation($task, $firstbody) {
//    	var $body = $firstbody;
//    	var state = { wasExpanding: undefined };
//    	var idle;
//    	var anim;
//    	
//    	var runAnimation = function() {
//    		state.wasExpanding = $task.attr("anim-invoke") === "expand";
//    		var wasExpanded = !state.wasExpanding;
////    		console.log("Was expanded: "+wasExpanded);
////    		console.log("State expanding: "+state.wasExpanding);
//    		
//    		$body = taskLoader.getBody($task);
//    		if (!wasExpanded) {
//    			taskLoader.insertBody($task);
//    			wasExpanded = true;
//    		}
//	    	var tween = TweenLite.from($body, slideDownTime, {height: "0px",
//	    		paused: true,
//	        	onUpdate: function() {
//	        		var expand = $task.attr("anim-invoke") === "expand";
////	        		console.log("Current expand: "+expand);
//	        		if(state.wasExpanding !== expand) {
////	        			console.log("reversing");
//	        			state.wasExpanding = expand;
//	        			tween.reverse();
//	        		}
//	        		else {
////	        			console.log("animating");
//	        		}
//	        	},
//	        	onComplete: function() {
////	        		console.log("expanded");
//	        		tween.kill();
//	        		idle.resume();
//		        },
//	        	onReverseComplete: function() {
//	        		if (wasExpanded) taskLoader.removeBody(taskLoader.getBody($task));
////	        		console.log("collapsed");
//	        		tween.kill();
//	        		idle.resume();
//	        	}
//		    });
//	    	return tween;
//    	}
//    	
//    	idle = TweenLite.from($body, 10000, {
//    		paused: true,
//        	onUpdate: function() {
//        		var expand = $task.attr("anim-invoke") === "expand";
//        		if(state.wasExpanding !== expand) {
////            		console.log("starting animation from idle");
//        			idle.pause();
//        			anim = runAnimation();
//        			anim.reverse();
//        			anim.resume();
//        		}
//        		else {
////            		console.log("idling");
//        		}
//        	},
//        	onComplete: function() {
//        		idle.play();
//	        }
//	    });
//    	
//    	anim = runAnimation(false);
//    	anim.resume();
//    }
    
    return {
    	
//    	collapse : function($task) {
//    		if($task !== undefined) {
//    			$task.attr("anim-invoke","collapse");
//    		}
//    	},
//    	
//    	expand : function($task) {
//    		if($task !== undefined) {
//	    		var hasAnimation = $task.attr("anim-state") === undefined;
//	    		$task.attr("anim-invoke","expand");
//	    		
//	    		if(hasAnimation) {
//	    			createAnimation($task, taskLoader.getBody($task));
//	    		}
//    		}
//    	},
        /**
         * Plays a slideUp animation and removes task detail DOM from page.
         */
        collapse : function($task) {
            if($task !== undefined && $task!== null) {
            	$task.removeClass("expanding");
            	$task.removeClass("expanded");
            	$task.addClass("collapsing");
            	
            	var $body = taskLoader.getBody($task);
            	var tween = TweenLite.to($body, slideUpTime, {height: "0px",
            		onUpdate: function() {
            			if (!$task.hasClass("collapsing")) {
            				tween.kill();
            			}
            		},
            		onComplete: function() {
            			$task.removeAttr("style");
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
        			taskLoader.insertBody($task);
        		}
        		$task.removeClass("collapsing");
        		$task.removeClass("collapsed");
        		$task.addClass("expanding");
            	
        		var $body = taskLoader.getBody($task);
        		
        		//TODO move following to css
        		$body.find(".elementComments:blank, .elementDetails:blank").hide();
        		$body.find(".commentForm").hide();
        		$body.find(".elementDelete").hide();
        		//TODO end
        		
        		addTaskFileTrees($task);
        		allVars.htmlPreProcessor.apply($task);
        		$task.css("border-width", "2px");
        		$task.css("padding-left", "6px");
        		$task.css("background-color", "#000");
        		$task.css("background-color", "rgba(0, 32, 48, 1)");
        		$body.show();

        		//reset varying height from collapse animation (stutter effect on fast clicking)
        		$body.css("height","");
                var tween = TweenLite.from($body, slideDownTime, {height: "0px",
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
};