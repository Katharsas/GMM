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

var Workbench = function() {
	var $workbench = $("#workbench");
	var $workbenchMenu = $workbench.find("#workbench-menu");
	var $workbenchTabs = $workbench.find("#workbench-tabs");
	
	var $loadButtons = $workbenchTabs.find(".workbench-load-typeButton");
	
	var taskLoader = TaskLoader(contextUrl + "/tasks/render", $("#workbench").find(".list-body"));
	var taskSwitcher;
	var expandedTasks;
	
	var render = function() {
		taskLoader.init();
		//TODO find better way to reset and reload without instantiating new stuff
		//TODO attach listeners to tasks on creation so the correct switcher can be called
		taskSwitcher = TaskSwitcher(taskLoader);
		expandedTasks = new Queue(3, function($task1, $task2) {
			return $task1[0] === $task2[0];
		});
	};
	var updateTasks = function() {
		Ajax.get(contextUrl + "/tasks/selected")
			.done(function(selected) {
				$loadButtons.each(function(index, element) {
					if (selected[index]) {
						$(element).addClass("selected");
					} else {
						$(element).removeClass("selected");
					}
				});
				render();
			});
	};
	this.load = function(type) {
		Ajax.post(contextUrl + "/tasks/load", { type: type })
			.done(updateTasks)
			.fail(showException);
	};
	var initWorkbenchTabMenu = function() {
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
	};
	var initWorkbenchTabs = function() {	
		
		var $loadForm = $workbenchTabs.find("form#workbench-loadForm");
		var $sortForm = $workbenchTabs.find("form#workbench-sortForm");
		var $searchForm = $workbenchTabs.find("form#workbench-searchForm");
		var $filterForm = $workbenchTabs.find("form#generalFilters");
		
		//-------------------------------------------------------
		//load form
		//-------------------------------------------------------
		$loadForm.find(".form-element").change(function() {
			Ajax.post(contextUrl + "/tasks/submitLoad", null, $loadForm);
		});
		
		//-------------------------------------------------------
		//sort form
		//-------------------------------------------------------
		$sortForm.find("select, input").change(function() {
			Ajax.post(contextUrl + "/tasks/submitSort", null, $sortForm)
				.done(//TODO only load new sorting data
						render);
		});
		
		//-------------------------------------------------------
		//search form
		//-------------------------------------------------------
		$searchForm.find(".workbench-search-submit").click(function() {
			Ajax.post(contextUrl + "/tasks/submitSearch", null, $searchForm)
				.done(render);
		});
		$searchForm.find("#workbench-search-switch").click(function() {
			setSearchType(!isEasySearch());
		});
		var $searchType = $("select#workbench-search-type");
		function isEasySearch() {
			return $searchType.val() === "true";
		}
		function setSearchType(isEasySearch) {
			$searchType.val(isEasySearch.toString());
			$searchForm.find("#workbench-search-easy").toggle(isEasySearch);
			$searchForm.find("#workbench-search-complex").toggle(!isEasySearch);
		}
		//init search visibility
		setSearchType(isEasySearch());
		
		//-------------------------------------------------------
		//filter form
		//-------------------------------------------------------
		var submitFilterForm = function() {
			Ajax.post(contextUrl + "/tasks/submitFilter", null, $filterForm)
				.done(render);
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
	initWorkbenchTabMenu();
	initWorkbenchTabs();
	updateTasks();
};



/**
 * This function is executed when document is ready for interactivity!
 */
$(document).ready(
	function() {
		tasksVars.edit = getURLParameter("edit");
		new TaskForm();
		
		workbench = new Workbench();
		
		
		//TODO sidebarmarker creation on task select
//			SidebarMarkers = SidebarMarkers(function() {
//				return $('<div>').html("Marker");
//			}, 2);
//			SidebarMarkers.registerSidebar("#page-tabmenu-spacer", true);
//			SidebarMarkers.addMarker("#test1");
//			SidebarMarkers.addMarker("#test2");

		
});


function switchListElement(element) {
	workbench.taskSwitcher.switchTask($(element).parent().first(), workbench.expandedTasks);
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
		var $type = $form.find("#taskForm-element-type select");
		switchPath($type);
		
		$type.change(function() {switchPath($type);});
		$new.click(function() {show();});
		$submit.click(function() {$form.submit();});
		$cancel.click(function() {
			//TODO reload only empty form
			alert(function() {
				tasksVars.edit = "";
				tasksFuncs.refresh();
			}, "TODO: Reset form only");
		});
	}
	
	function switchPath($taskElementType) {
		var selected = $taskElementType.find(":selected").val();
		var $path = $form.find("#taskForm-element-path");
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