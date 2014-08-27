var $currentElement = $();

$.expr[':'].blank = function(obj) {
	return !$.trim($(obj).text()).length;
};

var tasksVars = {
	"tab" : "",
	"edit" : "",
	"selectedTaskFileIsAsset" : ""
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
			
			new TaskForm();
			
			// hide .listElementBody
			collapseListElements();
			// hide comment and details when empty
			$(".elementComments:blank, .elementDetails:blank").hide();
			// hide comment input field & delete question
			$(".commentForm").hide();
			// hide delete question
			$(".elementDelete").hide();
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
});

/**
 * Handles taskForm behaviour.
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

function collapseListElements() {
	$(".listElementBody").hide();
}

function expandListElements() {
	$(".listElementBody").show();
}

TaskView = function(taskIdLink, taskDetailMap) {
	
	function show() {
		//TODO load task detail dom code
		//TODO add task detail dom to task head
		//TODO animate task detail
	}
	function hide() {
		//TODO animate task detail
		//TODO remove task detail dom code
	}
};

function switchListElement(element) {
    var slideDownTime = 0.5;
    var slideUpTime = 0.5;
    var $oldElement = $currentElement;
    var $newElement = $(element).parent().first();
    var isSameElement = $newElement[0] == $oldElement[0];
    	
    //hide old body
	var $oldBody = $oldElement.children(".listElementBody");
	TweenLite.to($oldBody, slideUpTime, {height: "0px", onComplete: function() {
			$oldBody.hide();
			$oldBody.css("height","");
			$oldElement.css("border-width", "0px");
	        $oldElement.css("padding-left", "8px");
	        removeTaskFileTrees($oldElement);
	        if(!isSameElement) {
	        	
	        }
        }
    });
    
	//show new body
	if (!isSameElement) {
		
		var $newBody = $(element).parent().children(".listElementBody");
		addTaskFileTrees($newElement);
        $newElement.css("border-width", "2px");
        $newElement.css("padding-left", "6px");
		$newBody.show();
        TweenLite.from($newBody, slideDownTime, {height: "0px", onComplete: function() {
        		$newBody.css("height","");
	        }
	    });
        $currentElement = $newElement;
	}
	else {
		$newElement = $();
	}
    
}

function addTaskFileTrees($element) {
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
}

function removeTaskFileTrees($element) {
	$element.find('#assetFilesContainer').empty();
	$element.find('#wipFilesContainer').empty();
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
	var comment = $("#confirmDialogTextArea").attr("value");
	var url = "editComment/" + taskId + "/" + commentId;
	$.post(url, {"editedComment" : comment}, 
			function() {window.location.reload();}
	);
}

/**
 * @param isEasySearch -
 *            String or boolean
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
	var uri = "tasks/download/" + idLink + "/" + tasksFuncs.subDir() + "/" + dir + "/" + tasksFuncs.tabPar();
	window.open(uri);
}

function confirmDeleteFile(idLink) {
	var dir = tasksFuncs.filePath();
	if (dir === undefined || dir === "") {
		return;
	}
	confirm(
			function() {
				var assetPar = "&asset=" + tasksVars.selectedTaskFileIsAsset.toString();
				$.post("tasks/deleteFile/" + idLink + tasksFuncs.tabPar() + assetPar, {
					dir : dir
				}, function() {
					tasksFuncs.refresh();
				});
			}, "Delete " + tasksFuncs.filePath() + " ?");
}

function confirmDeleteTask(idLink, name) {
	confirm(function() {
		window.location = "tasks/deleteTask/" + idLink + tasksFuncs.tabPar();
	}, "Delete task \'" + name + "\' ?");
}