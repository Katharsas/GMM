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

//add listeners to global scope
(function() {
	var ls = TaskListeners(tasksVars, tasksFuncs);
	for (var func in ls) {
		window[func] = ls[func];
	}
})();


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
			//TODO highlight currently selected load types
//			tasksVars.edit = getURLParameter("edit");
//			var $activeTab = $(".subTabmenu .tab a[href=\"tasks" + tasksFuncs.tabPar() + "\"]").parent();
//			$activeTab.addClass("activeSubpage");
			
			TaskLoader = TaskLoader(allVars.contextPath+"/tasks/render", $("#workbench").find(".list-body"));
			TaskSwitcher = TaskSwitcher(TaskLoader);
			new TaskForm();
			tasksVars.expandedTasks = new Queue(3, function($task1, $task2) {
				return $task1[0] === $task2[0];
			});
			
			//TODO sidebarmarker creation on task select
//			SidebarMarkers = SidebarMarkers(function() {
//				return $('<div>').html("Marker");
//			}, 2);
//			SidebarMarkers.registerSidebar("#page-tabmenu-spacer", true);
//			SidebarMarkers.addMarker("#test1");
//			SidebarMarkers.addMarker("#test2");
			
			//TODO setup search and filters and sort
//			// set Search according to selected search type (easy or complex)
			setSearchVisibility($("#searchTypeSelect").val());
//			// hide search type selector
			$("#searchTypeSelect").hide();
//			// hide filter submit
//			$("#generalFiltersInvisible").hide();
//			// hide generalFilterBody
//			if ($("#generalFiltersHidden").is(":checked"))
//				toggleGeneralFilters();
//			toggleSpecificFilters();// TODO
		
			// listener
			$(".submitSearchButton").click(function() {
				$("#searchForm").submit();
			});
//			$(".sortFormElement").change(function() {
//				$("#sortForm").submit();
//			});
//			$("#generalFiltersAllCheckbox").change(function() {
//				switchGeneralFiltersAll($(this));
//			});
//			$(".generalFiltersFormElement").change(function() {
//				submitGeneralFilters();
//			});
});

function switchListElement(element) {
	TaskSwitcher.switchTask($(element).parent().first(), tasksVars.expandedTasks);
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

function submitGeneralFilters() {
	$(".generalFilters").submit();
}


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