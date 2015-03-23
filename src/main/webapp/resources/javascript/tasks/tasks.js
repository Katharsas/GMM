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
		var url = contextUrl + "/tasks";
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
	workbench.taskLoader = TaskLoader(contextUrl + "/tasks/render", $("#workbench").find(".list-body"));
	workbench.render();
};
workbench.load = function(type) {
	Ajax.get(contextUrl + "/tasks/load", { type: type })
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
workbench.initWorkbenchTabs = function() {
	new WorkbenchTabs();
	
	var $loadForm = $("form#workbench-loadForm");
	var $sortForm = $("form#workbench-sortForm");
	var $searchForm = $("form#workbench-searchForm");
	var $filterForm = $("form#generalFilters");
	
	//load form
	$loadForm.find(".form-element").change(function() {
		Ajax.post(contextUrl + "/tasks/submitLoad", null, $loadForm);
	});
	//sort form
	$sortForm.find("select, input").change(function() {
		Ajax.post(contextUrl + "/tasks/submitSort", null, $sortForm)
			.done(//TODO only load new sorting data
					workbench.render);
	});
	//search form
	$searchForm.find(".submitSearchButton").click(function() {
		Ajax.post(contextUrl + "/tasks/submitSearch", null, $searchForm)
			.done(workbench.render);
	});
	//filter form
	var submitFilterForm = function() {
		Ajax.post(contextUrl + "/tasks/submitFilter", null, $filterForm)
			.done(workbench.render);
	};
	$filterForm.find("input[type='checkbox']").not("#generalFilters-all").change(function() {
		submitFilterForm();
	});
	//filter form (all checkbox binding)
	(function() {
		var $all = $filterForm.find("#generalFilters-all");
		var $checkboxes = $filterForm.find(".generalFilters-all-target");
		var cbg = new CheckboxGrouper($checkboxes, function(areChecked) {
			$all.prop("checked", areChecked);
		});
		$all.change(function() {
			var isChecked = $all.prop("checked");
			cbg.changeGroup(isChecked);
			submitFilterForm();
		});
	})();
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

		workbench.initWorkbenchTabs();
		
		// set Search according to selected search type (easy or complex)
//		setSearchVisibility($("#searchTypeSelect").val());	
		// hide search type selector
//		$("#searchTypeSelect").hide();
	
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