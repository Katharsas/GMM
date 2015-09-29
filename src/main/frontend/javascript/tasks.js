/* jshint esnext:true */
import $ from "./lib/jquery";
import Ajax from "./shared/ajax";
import Dialogs from "./shared/dialogs";
import Queue from "./shared/queue";
import TaskLoader from "./shared/taskloader";
import TaskSwitcher from "./shared/taskswitcher";
import TaskEventBindings from "./shared/tasklisteners";
import { contextUrl, allVars, getURLParameter } from "./shared/default";

var tasksVars = {
	"edit" : "",
	"selectedTaskFileIsAsset" : "",
};

var tasksFuncs = {
	"subDir" : function() {
		return tasksVars.selectedTaskFileIsAsset ? "asset" : "other";
	},
	"filePath" : function() {
		return allVars["task-files-selected"].attr("rel");
	},
	"refresh" : function() {
		var url = contextUrl + "/tasks";
		if (tasksVars.edit !== "") {
			url += "?edit=" + tasksVars.edit;
		}
		window.location.href =  url;
	}
};

tasksVars.tab = global.tasksHTML.tab;

/*
 * ////////////////////////////////////////////////////////////////////////////////
 * FUNCTIONS
 * ////////////////////////////////////////////////////////////////////////////////
 */

/**
 * TODO: call delete and update methods on taskloader after deleting/editing
 *  => route tasks listeners from tasks in workbench to workbench somehow
 * 
 * @author Jan Mothes
 */
var Workbench = function() {
	var that = this;
	
	var $workbench = $("#workbench");
	var $workbenchList = $workbench.find(".list-body");
	var $workbenchMenu = $workbench.find("#workbench-menu");
	var $workbenchTabs = $workbench.find("#workbench-tabs");
	
	var $loadButtons = $workbenchTabs.find(".workbench-load-typeButton");
	var $count = $workbenchList.find(".list-count span");
	
	var taskListId = "workbench";
	var taskLoader = TaskLoader;
	taskLoader.registerTaskList(taskListId, {
		$list : $workbenchList,
		url : "/tasks/workbench",
	});
	
	var taskSwitcher = TaskSwitcher(taskListId, taskLoader);
	var expandedTasks =  new Queue(3, function($task1, $task2) {
		return $task1[0] === $task2[0];
	});
	
	var taskBinders = TaskEventBindings(tasksVars, tasksFuncs,
		function($task) {
			taskSwitcher.switchTask($task, expandedTasks);
		},
		function($task) {
			taskLoader.updateTask(taskListId, $task);
		},
		function($task) {
			taskLoader.removeTask($task);
		}
	);
	taskLoader.setTaskEventBinders(taskListId, taskBinders);
	
	var render = function() {
		taskLoader.createTaskList(taskListId, function() {
			$count.text(taskLoader.getTaskIds.length);
			this.expandedTasks.clear();
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
			.done(updateTasks);
	};
	var initWorkbenchTabMenu = function() {
		var $menuTabs = $workbenchMenu.find(".workbench-menu-tab");
		var $tabs = $workbenchTabs.find(".workbench-tab");
		
		var tabWidthPercent = 100 / $menuTabs.length;
		$menuTabs.css("width", tabWidthPercent + "%");
		
		$menuTabs.last().addClass("workbench-menu-tab-last");
		
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
		
		//-------------------------------------------------------
		//load tab
		//-------------------------------------------------------
		var $loadForm = $workbenchTabs.find("form#workbench-loadForm");
		
		$loadForm.find(".form-element").change(function() {
			Ajax.post(contextUrl + "/tasks/submitLoad", null, $loadForm);
		});
		
		//-------------------------------------------------------
		//sort tab
		//-------------------------------------------------------
		var $sortForm = $workbenchTabs.find("form#workbench-sortForm");
		
		$sortForm.find("select, input").change(function() {
			Ajax.post(contextUrl + "/tasks/submitSort", null, $sortForm)
				.done(//TODO only load new sorting data
						render);
		});
		
		//-------------------------------------------------------
		//search tab
		//-------------------------------------------------------
		var $searchForm = $workbenchTabs.find("form#workbench-searchForm");
		
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
		//filter tab
		//-------------------------------------------------------
		var $filterForm = $workbenchTabs.find("form#generalFilters");
		
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
		
		//-------------------------------------------------------
		//admin tab
		//-------------------------------------------------------
		var $saveTasks = $("#dialog-saveTasks");
		var $saveTasksForm = $saveTasks.find("#dialog-saveTasks-form");
		
		$saveTasks.find("#dialog-saveTasks-saveButton").click(function() {
			Ajax.post(contextUrl + "/tasks/workbench/admin/save", {}, $saveTasksForm)
				.done(function() {
					Dialogs.hideDialog($("#dialog-saveTasks"));
				});
		});
		$workbenchTabs.find("#workbench-admin-saveButton").click(function() {
			Dialogs.showDialog($saveTasks);
		});
		$workbenchTabs.find("#workbench-admin-deleteButton").click(function() {
			var $confirm = confirm(function() {
				Dialogs.hideDialog($confirm);
				Ajax.post(contextUrl + "/tasks/workbench/admin/delete")
					.done(function(){
						taskLoader.removeTasks(taskLoader.getTaskIds(taskListId));
					});
			}, "Delete all tasks currently visible in workbench?");
		});
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
		
		var workbench = new Workbench();
		global.workbench = workbench;
		
		//TODO sidebarmarker creation on task select
//			SidebarMarkers = SidebarMarkers(function() {
//				return $('<div>').html("Marker");
//			}, 2);
//			SidebarMarkers.registerSidebar("#page-tabmenu-spacer", true);
//			SidebarMarkers.addMarker("#test1");
//			SidebarMarkers.addMarker("#test2");
});

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