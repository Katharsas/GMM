//adds :blank selector to jQuery
$.expr[':'].blank = function(obj) {
	return !$.trim($(obj).text()).length;
};

//TODO use big Tasks object to store these variables and all methods of this js file
//TODO put all handlers/listener callbacks into Handler object in Tasks object
var tasksVars = {
	"tab" : "",
	"edit" : "",
	"selectedTaskFileIsAsset" : "",
	"expandedTasks" : undefined,
	"taskLoader" : undefined
};

var tasksFuncs = {
	"tabPar" : function() {
		return "?tab=" + (tasksVars.tab === undefined || tasksVars.tab === null ? "" : tasksVars.tab);
	},
	"editPar" : function() {
		return "&edit=" + (tasksVars.edit === undefined || tasksVars.edit === null ? "" : tasksVars.edit);
	},
	"subDir" : function() {
		return tasksVars.selectedTaskFileIsAsset ? "asset" : "other";
	},
	"filePath" : function() {
		return allVars.selectedTaskFile.attr("rel");
	},
	"refresh" : function() {
		window.location.href = "tasks" + tasksFuncs.tabPar() + "&edit=" + tasksVars.edit;
	}
};

/*
 * ////////////////////////////////////////////////////////////////////////////////
 * FUNCTIONS
 * ////////////////////////////////////////////////////////////////////////////////
 */

/**
 * This function is executed when document is ready for interactivity!
 */
$(document).ready(
		function() {
			// get subTab and set as active tab / others as inactivetabs
			tasksVars.tab = getURLParameter("tab");
			tasksVars.edit = getURLParameter("edit");
			var $activeTab = $(".subTabmenu .tab a[href=\"tasks" + tasksFuncs.tabPar() + "\"]").parent();
			$activeTab.addClass("activeSubpage");
			
			TaskLoader = TaskLoader();
			new TaskForm();
			
			// set Search according to selected search type (easy or complex)
			setSearchVisibility($("#searchTypeSelect").val());
			// hide search type selector
			$("#searchTypeSelect").hide();
			// hide filter submit
			$("#generalFiltersInvisible").hide();
			// hide generalFilterBody
			if ($("#generalFiltersHidden").is(":checked"))
				toggleGeneralFilters();
			toggleSpecificFilters();// TODO
		
			// listener
			$(".submitSearchButton").click(function() {
				$("#searchForm").submit();
			});
			$(".sortFormElement").change(function() {
				$("#sortForm").submit();
			});
			$("#generalFiltersAllCheckbox").change(function() {
				switchGeneralFiltersAll($(this));
			});
			$(".generalFiltersFormElement").change(function() {
				$(".generalFilters").submit();
			});
			
			//default is 60
			TweenLite.ticker.fps(30);
			
			tasksVars.expandedTasks = new TaskQueue(3, function($task1, $task2) {
				return $task1[0] === $task2[0];
			});
});


function switchListElement(element) {
    var $newElement = $(element).parent().first();
    var isAlreadyExpanded = tasksVars.expandedTasks.contains($newElement);
    
    var $collapse;
    var $expand;
    
    //define which element needs to be expanded/collapsed and add/remove them from expandedTasks queue
    if(isAlreadyExpanded) {
    	$expand = undefined;
    	$collapse = $newElement;
    	tasksVars.expandedTasks.remove($collapse);
    }
    else {
    	$expand = $newElement;
    	$collapse = tasksVars.expandedTasks.add($expand);
    }
    TaskSwitcher.collapse($collapse);
    TaskSwitcher.expand($expand);
}

function switchDeleteQuestion(element) {
	var $delete = $(element).parent().parent().children(".elementDelete");
	$delete.toggle();
}

function findSwitchCommentForm(element) {
	switchCommentForm($(element).parents(".listElementBody").find(
			".commentForm"));
}
function switchCommentForm($commentForm) {
	if ($commentForm.is(":visible")) {
		hideCommentForm($commentForm);
	} else {
		showCommentForm($commentForm);
	}
}
function hideCommentForm($commentForm) {
	var $elementComments = $commentForm.parent();
	if ($elementComments.is(":visible:blank")) {
		$elementComments.hide();
	}
	$commentForm.hide();
}
function showCommentForm($commentForm) {
	$commentForm.parent().show();
	$commentForm.show();
}

function changeComment(comment, taskId, commentId) {
	confirm(function() {confirmCommentChange(taskId, commentId);},
			"Bitte Kommentar ändern",
			undefined,
			comment);
}

function confirmCommentChange(taskId, commentId) {
	var comment = $("#confirmDialogTextArea").val();
	var url = "editComment/" + taskId + "/" + commentId;
	$.post(url, {"editedComment" : comment}, 
			function() {window.location.reload();}
	);
}

/**
 * @param isEasySearch - String or boolean
 */
function setSearchVisibility(isEasySearch) {
	var $search = $(".search");
	if (isEasySearch.toString() === "true") {
		$search.find(".complexSearch").hide();
		$search.find(".easySearch").show();
	} else {
		$search.find(".complexSearch").show();
		$search.find(".easySearch").hide();
	}
}

function switchSearchType() {
	var easySearch = $("#searchTypeSelect").val();
	var newEasySearch = (easySearch !== "true").toString();
	$("#searchTypeSelect").val(newEasySearch);
	setSearchVisibility(newEasySearch);
}

function toggleFilters($toggle, $resize) {
	if ($toggle.is(":visible")) {
		$toggle.hide();
		// $toggle.animate({left:'400px'},900);
		// $toggle.hide();

		$resize.css("width", "2em");
		return true;
	}
	$toggle.show();
	// $toggle.animate({left:'0px'},900);
	$resize.css("width", "9em");
	return false;
}

function toggleGeneralFilters() {
	return toggleFilters($("#generalFilterBody"), $(".generalFilters"));
}
function toggleSpecificFilters() {
	return toggleFilters($("#specificFilterBody"), $(".specificFilters"));
}

function switchGeneralFilters() {
	$("#generalFiltersHidden").prop("checked", toggleGeneralFilters());
	submitGeneralFilters();
}

function switchSpecificFilters() {
	// TODO
	toggleSpecificFilters();
}

function switchGeneralFiltersAll($element) {
	$(".generalFiltersAllCheckBoxTarget").attr("checked",
			$element.is(":checked"));
	submitGeneralFilters();
}

function uploadFile(input, idLink) {
	allVars.$overlay.show();
	
	var file = input.files[0];
	var uri = "tasks/upload/" + idLink + tasksFuncs.tabPar();

	sendFile(file, uri, function(responseText) {
		tasksFuncs.refresh();
//		alert(allVars.$overlay.show, "Upload successfull!");
	});
}

function downloadFromPreview(idLink, version) {
	var uri = "tasks/download/" + idLink + "/preview/" + version + "/" + tasksFuncs.tabPar();
	window.open(uri);
}

function downloadFile(idLink) {
	var dir = tasksFuncs.filePath();
	if (dir === undefined || dir === "") {
		return;
	}
	var uri = "tasks/download/" + idLink + "/" + tasksFuncs.subDir() + "/" + dir + "/";
	window.open(uri);
}

function confirmDeleteFile(idLink) {
	var dir = tasksFuncs.filePath();
	if (dir === undefined || dir === "") {
		return;
	}
	confirm(
			function() {
				$.post("tasks/deleteFile/" + idLink, {dir: dir,
						asset: tasksVars.selectedTaskFileIsAsset.toString()},
					function() {
						tasksFuncs.refresh();
				});
			}, "Delete " + tasksFuncs.filePath() + " ?");
}

function confirmDeleteTask(idLink, name) {
	confirm(function() {
		window.location = "tasks/deleteTask/" + idLink + tasksFuncs.tabPar();
	}, "Delete task \'" + name + "\' ?");
}

//var getUri = function(url, parameters) {
//	var fullUri = url;
//	var first = true;
//	if (parameters !== undefined && parameters!== null) {
//		console.log("Parameters:");
//		for (key in parameters) {
//			var parameter = key + "=" + encodeURIComponent(parameters[key]);
//			console.log(parameter);
//			if (first) {
//				fullUri += "?"+parameter;
//				first = false;
//			} else {
//				fullUri += "&"+parameter;
//			}
//		}
//	}
//	console.log(fullUri);
//	return fullUri;
//};


/**
 * -------------------- TaskLoader ----------------------------------------------------------------
 * Static (called when document ready)
 * Accepts callback which will be executed when all tasks are loaded
 */
var TaskLoader = function(onLoaded) {
	this.tasks = undefined;
	reloadAndInsert();
	
	function reloadAndInsert() {
		$.getJSON("render").done(function(taskRenders) {
			tasks = taskRenders;
			insertHeaders();
			if(onLoaded !== undefined) onLoaded();
		}).fail(showException);
	}
	
	function insertHeaders() {
		var headerString = "";
		tasks.forEach(function (task) {
			headerString = headerString + task.header + "\n";
		});
		$("#listsMain").append(headerString);
	}
	
	return {
		
		insertBody : function ($task) {
			var idLink = $task.attr('id');
			var body = undefined;
			tasks.some(function(task) {
				if(task.idLink === idLink) {
					body = task.body;
					return true;
				}
				return false;
			});
			$task.append(body);
		},
		
		removeBody : function ($task) {
			$task.children(":last-child").remove();
		}
	};
};

/**
 * -------------------- TaskForm -----------------------------------------------------------------
 * Initializes task form and registers behaviour for task form buttons.
 */
function TaskForm() {
	var $form = $("#taskForm");
	var $submit = $("#submitTaskButton");
	var $cancel = $("#cancelTaskButton");
	var $new = $("#newTaskButton");
	init();
	
	function init() {
		if (tasksVars.edit !== "") {
			show();
			$form.find("#taskGroupType").hide();
		}
		var $type = $form.find("#taskElementType select");
		switchPath($type);
		
		$type.change(function() {switchPath($type);});
		$new.click(function() {show();});
		$submit.click(function() {$form.submit();});
	}
	
	function switchPath($taskElementType) {
		var selected = $taskElementType.find(":selected").val();
		var $path = $form.find("#taskElementPath");
		switch(selected) {
			case "GENERAL":	$path.hide();break;
			default:		$path.show();break;
		}
	}
	
	function show() {
		$form.show();
		$submit.show();
		$cancel.show();
		$new.hide();
	}
}

/**
 * -------------------- TaskSwitcher -------------------------------------------------------------
 * Provides the two static methods collapse and expand, which load and animate a tasks details.
 */
var TaskSwitcher = function() {
	this.slideDownTime = 0.5;
    this.slideUpTime = 0.5;
    
    var getBody = function($task) {
    	return $task.children(".listElementBody");
    };
    
    var addTaskFileTrees = function($element) {
    	var idLink = $element.attr('id');
    	var url = idLink + tasksFuncs.tabPar();
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
            	        TaskLoader.removeBody($task);
                    }
                });
            }
        },
        
        /**
         * Adds task detail DOM to page and plays a slideDown animation. 
         */
        expand : function($task) {
        	if ($task !== undefined) {
        		TaskLoader.insertBody($task);
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
        }
    };
}();


/**
 * -------------------- TaskQueue ---------------------------------------------------------------
 * @param maxSize - Maximum number of elements in the queue. If undefined, queue size is unlimited.
 * Modifiable propety. If the size is reduced, surplus elements will not be removed.
 * @param taskComparator - Function which takes to tasks as arguments and returns true if they are
 * considered to be the same task. Used by remove & contains function.
 */
function TaskQueue(maxSize, taskComparator) {
	var queue = [];
	this.maxSize = maxSize;
	
	/**
	 * Adds an element to the queue. If the queue is full, the oldest element will be removed.
	 * @returns the removed element or undefined, if no element was removed.
	 */
	this.add = function(task) {
		var result = undefined;
		if(this.maxSize !== undefined && queue.length >= this.maxSize) {
			result = queue.pop();
		}
		queue.unshift(task);
		return result;
	};
	
	var containsRemove = function(task, remove) {
		for (var i = 0; i < queue.length; i++) {
			if (taskComparator(queue[i], task)) {
				if(remove) queue.splice(i, 1);
				return true;
			}
		}
		return false;
	};
	
	/**
	 * Compares the given element with the queued elements by using it as second parameter with the
	 * given taskComparator.
	 * @returns true, if the element was found
	 */
	this.contains = function(task) {
		return containsRemove(task, false);
	};
	
	/**
	 * Compares the given element with the queued elements by using it as second parameter with the
	 * given taskComparator and removes it if found.
	 * @returns true, if the element was found and removed
	 */
	this.remove = function(task) {
		return containsRemove(task, true);
	};
	
	/**
	 * @return array with all elements ordered by insertion order (latest first, oldest last).
	 */
	this.get = function() {
		return queue.slice();
	};
}