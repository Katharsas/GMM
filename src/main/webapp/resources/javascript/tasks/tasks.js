var tasksVars = {
	"edit" : "",
	"selectedTaskFileIsAsset" : "",
};

var tasksFuncs = {
	"subDir" : function() {
		return tasksVars.selectedTaskFileIsAsset ? "asset" : "other";
	},
	"filePath" : function() {
		return allVars.selectedTaskFile.attr("rel");
	},
	"refresh" : function() {
		var url = allVars.contextPath + "/tasks";
		if (tasksVars.edit !== "") {
			url += "?edit=" + tasksVars.edit;
		}
		window.location.href =  url;
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

var workbench = {};

workbench.init = function() {
	workbench.taskLoader = TaskLoader(allVars.contextPath+"/tasks/render", $("#workbench").find(".list-body"));
	workbench.render();
}
workbench.load = function(type) {
	$.post(allVars.contextPath+"/load", { type: type })
		.done(function() {
			workbench.render();
		})
		.fail(showException);
}
workbench.render = function() {
	workbench.taskLoader.init();
	//TODO find better way to reset and reload without instantiating new stuff
	//TODO attach listeners to tasks on creation so the correct switcher can be called
	workbench.taskSwitcher = TaskSwitcher(workbench.taskLoader);
	workbench.expandedTasks = new Queue(3, function($task1, $task2) {
		return $task1[0] === $task2[0];
	});
}


/**
 * This function is executed when document is ready for interactivity!
 */
$(document).ready(
	function() {
		// get subTab and set as active tab / others as inactivetabs
		//TODO highlight currently selected load types
		tasksVars.edit = getURLParameter("edit");
//			var $activeTab = $(".subTabmenu .tab a[href=\"tasks" + tasksFuncs.tabPar() + "\"]").parent();
//			$activeTab.addClass("activeSubpage");
		
		workbench.init();
		new TaskForm();
		
		//TODO sidebarmarker creation on task select
//			SidebarMarkers = SidebarMarkers(function() {
//				return $('<div>').html("Marker");
//			}, 2);
//			SidebarMarkers.registerSidebar("#page-tabmenu-spacer", true);
//			SidebarMarkers.addMarker("#test1");
//			SidebarMarkers.addMarker("#test2");

		//listener
		
		
		
		// set Search according to selected search type (easy or complex)
		setSearchVisibility($("#searchTypeSelect").val());	
		// hide search type selector
		$("#searchTypeSelect").hide();
		// hide filter submit
		$("#generalFiltersInvisible").hide();
		// hide generalFilterBody
		if ($("#generalFiltersHidden").is(":checked")) toggleGeneralFilters();
	
		//workbench listener
		$(".submitSearchButton").click(function() {
			$("#searchForm").submit();
		});
		$("form#workbench-loadForm").find(".form-element").change(function() {
			$("form#workbench-loadForm").submit();
		});
		$(".sortFormElement").change(function() {
			$("#sortForm").submit();
		});
		$("#generalFiltersAllCheckbox").change(function() {
			switchGeneralFiltersAll($(this));
		});
		$(".generalFiltersFormElement").change(function() {
			submitGeneralFilters();
		});
});



function switchListElement(element) {
	workbench.taskSwitcher.switchTask($(element).parent().first(), workbench.expandedTasks);
};

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
	return toggleFilters($("#generalFilterBody"), $("#generalFilters"));
}

function switchGeneralFilters() {
	$("#generalFiltersHidden").prop("checked", toggleGeneralFilters());
	submitGeneralFilters();
}

function switchGeneralFiltersAll($element) {
	$(".generalFiltersAllCheckBoxTarget").attr("checked",
			$element.is(":checked"));
	submitGeneralFilters();
}

function submitGeneralFilters() {
	$("#generalFilters").ajaxSubmit().data('jqxhr')
		.fail(showException)
		.done(workbench.render);
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
		$cancel.click(function() {
			//TODO reload only empty form
			tasksVars.edit = "";
			tasksFuncs.refresh();
		});
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