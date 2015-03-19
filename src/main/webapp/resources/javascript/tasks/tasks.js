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
};
workbench.load = function(type) {
	$.post(allVars.contextPath+"/load", { type: type })
		.done(function() {
			workbench.render();
		})
		.fail(showException);
};
workbench.render = function() {
	workbench.taskLoader.init();
	//TODO find better way to reset and reload without instantiating new stuff
	//TODO attach listeners to tasks on creation so the correct switcher can be called
	workbench.taskSwitcher = TaskSwitcher(workbench.taskLoader);
	workbench.expandedTasks = new Queue(3, function($task1, $task2) {
		return $task1[0] === $task2[0];
	});
};


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

		//workbench menu setup
		new WorkbenchTabs();
		
		//workbench-filter setup
		var $filters = $("#generalFilters");
		var $all = $filters.find("#generalFilters-all");
		var $checkboxes = $filters.find(".generalFilters-all-target");
		var cbg = new CheckboxGrouper($checkboxes, function(areChecked) {
			$all.prop("checked", areChecked);
		});
		$all.change(function() {
			var isChecked = $all.prop("checked");
			cbg.changeGroup(isChecked);
			submitGeneralFilters();
		});
		$filters.find("input[type='checkbox']").not("#generalFilters-all").change(function() {
			submitGeneralFilters();
		});
		
		
		// set Search according to selected search type (easy or complex)
//		setSearchVisibility($("#searchTypeSelect").val());	
		// hide search type selector
		$("#searchTypeSelect").hide();
	
		//workbench form submit
		$(".submitSearchButton").click(function() {
			$("form#workbench-searchForm").submit();
		});
		$("form#workbench-loadForm").find(".form-element").change(function() {
			$("form#workbench-loadForm").submit();
		});
		$("form#workbench-sortForm").find("select, input").change(function() {
			$("form#workbench-sortForm").submit();
		});
});



function switchListElement(element) {
	workbench.taskSwitcher.switchTask($(element).parent().first(), workbench.expandedTasks);
}

///**
// * @param isEasySearch - String or boolean
// */
//function setSearchVisibility(isEasySearch) {
//	var $search = $(".search");
//	if (isEasySearch.toString() === "true") {
//		$search.find(".complexSearch").hide();
//		$search.find(".easySearch").show();
//	} else {
//		$search.find(".complexSearch").show();
//		$search.find(".easySearch").hide();
//	}
//}

function switchSearchType() {
	var easySearch = $("#searchTypeSelect").val();
	var newEasySearch = (easySearch !== "true").toString();
	$("#searchTypeSelect").val(newEasySearch);
	setSearchVisibility(newEasySearch);
}

function submitGeneralFilters() {
	var url = allVars.contextPath + "/tasks/submitFilter";
	$("#generalFilters").ajaxSubmit({url:url, type:"post"}).data('jqxhr')
		.fail(showException)
		.done(workbench.render);
}

/**
 * TODO: move all workbench stuff into this and rename it
 */
function WorkbenchTabs() {
	var $workbench = $("#workbench");
	var $workbenchMenu = $workbench.find("#workbench-menu");
	var $workbenchTabs = $workbench.find("#workbench-tabs");
	
	var $menuTabs = $workbenchMenu.find(".workbench-menu-tab");
	var $tabs = $workbenchTabs.find(".workbench-tab");
	
	var tabWidthPercent = 100 / $menuTabs.length;
	$menuTabs.css("width", tabWidthPercent + "%");
	
	$menuTabs.each(function(index, tab) {
		var $tab = $(tab);
		$tab.click(onTabClick(index));
	});
	$menuTabs.first().trigger("click");
	
	function onTabClick(index) {
		return function () {
			$menuTabs.removeClass("workbench-menu-tab-active");
			$(this).addClass("workbench-menu-tab-active");
			$tabs.hide();
			$tabs.eq(index).show();
		};
	}
}

/**
 * Links a controller to a group of checkboxes.
 * 
 * @param $checkboxGroup - all checkboxes of the controlled group
 * @param onGroupChange - function which will get called by the CheckboxGrouper
 * to update the controller when the checkbox group reaches complete un-/checked
 * state by single changes. Should accept a boolean parameter (un/checked).
 */
function CheckboxGrouper($checkboxGroup, onGroupChange) {
	
	$checkboxGroup.change(function($element) {
		if(!$checkboxGroup.is(':not(:checked)')) {
			onGroupChange(true);
		}
		else if (!$checkboxGroup.is(":checked")) {
			onGroupChange(false);
		}
	});
	
	/**
	 * Call this when controller wants to un-/check the group.
	 * @param isChecked - true if controller wants to check, false if uncheck
	 */
	this.changeGroup = function(isChecked) {
		$checkboxGroup.prop("checked", isChecked);
	};
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