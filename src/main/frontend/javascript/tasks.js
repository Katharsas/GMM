import $ from "./lib/jquery";
import Ajax from "./shared/ajax";
import Dialogs from "./shared/dialogs";
import TaskForm from "./shared/TaskForm";
import TaskCache from "./shared/TaskCache";
import TaskList from "./shared/TaskList";
import TaskEventBindings from "./shared/tasklisteners";
import { contextUrl, getURLParameter } from "./shared/default";

var tasksVars = {
	"edit" : "",
};

tasksVars.tab = global.tasksHTML.tab;
global.tasksVars = tasksVars;

/*
 * ////////////////////////////////////////////////////////////////////////////////
 * FUNCTIONS
 * ////////////////////////////////////////////////////////////////////////////////
 */

/**
 * -------------------- Workbench ----------------------------------------------------------------
 * List of tasks with options for loading, sorting, filtering and searching tasks in(to) this list.
 * 
 * @author Jan Mothes
 */
var Workbench = function(taskForm) {
	
	var $workbench = $("#workbench");
	var $workbenchList = $workbench.find(".list-body");
	var $tabs = $workbench.find("#workbench-tabs .workbench-tab");
	var tabIdsToMenuTabs = {};
	
	var $loadButtons = $tabs.find(".workbench-load-typeButton");
	var $count = $workbenchList.find(".list-count span");
	
	var taskBinders = TaskEventBindings(taskForm.prepareEdit);
	var taskCache =  TaskCache("/tasks/workbench/renderTaskData");
	
	var taskListSettings = {
		$list : $workbenchList,
		eventUrl : "/tasks/workbench/taskListEvents",
		eventBinders : taskBinders,
		onChange : function(newSize) {
			$count.text(newSize);
		}
	};
	var taskList = TaskList(taskListSettings, taskCache);

	taskForm.setOnEdit(function(id) {
		taskList.markTaskDeprecated(null, id);
	});
	taskForm.setOnCreate(taskList.update);
	
	var updateTasks = function() {
		Ajax.get(contextUrl + "/tasks/selected")
			.then(function(selected) {
				$loadButtons.each(function(index, element) {
					if (selected[index]) {
						$(element).addClass("selected");
					} else {
						$(element).removeClass("selected");
					}
				});
			});
	};
	this.load = function(type) {
		Ajax.post(contextUrl + "/tasks/load", { type: type })
			.then(updateTasks);
	};
	var initWorkbenchTabMenu = function() {
		var $menuTabs = $workbench.find("#workbench-menu .workbench-menu-tab");
		
		var tabWidthPercent = 100 / $menuTabs.length;
		$menuTabs.css("width", tabWidthPercent + "%");
		$menuTabs.last().addClass("workbench-menu-tab-last");
		
		$tabs.each(function() {
			var $tab = $(this);
			var tabId = $tab.data("tabid");
			var $menuTab = $menuTabs.filter("[data-tabid='" + tabId + "']");
			tabIdsToMenuTabs[tabId] = { $tab: $tab, $menuTab: $menuTab };
			$menuTab.hover(function() {
				$menuTab.addClass("hover-active");
			}, function() {
				$menuTab.removeClass("hover-active");
			});
			$menuTab.click(onTabClick(tabId));
		});
		
		$menuTabs.first().trigger("click");
		
		function onTabClick(tabId) {
			return function () {
				$menuTabs.removeClass("workbench-menu-tab-active");
				$(this).addClass("workbench-menu-tab-active");
				$tabs.hide();
				var $tab = $tabs.filter("[data-tabid='" + tabId + "']");
				$tab.show();
			};
		}
	};
	var highlightMenuTab = function(tabId, isActive, callback) {
		var $menuTab = tabIdsToMenuTabs[tabId].$menuTab;
		var $highlight;
		if(isActive) {
			$highlight = $("<div>X</div>").addClass("workbench-menu-tab-highlight");
			$highlight.on("mouseover", function(event) {
				event.stopPropagation();
			});
			$highlight.on("click", function(event) {
				event.stopPropagation();
				callback();
			});
			$menuTab.append($highlight);
		} else {
			$highlight = $menuTab.find(".workbench-menu-tab-highlight");
			$highlight.remove();
		}
		
	};
	var initWorkbenchTabs = function() {	
		
		//-------------------------------------------------------
		//load tab
		//-------------------------------------------------------
		var $loadForm = $tabs.find("form#workbench-loadForm");
		
		$loadForm.find(".form-element").change(function() {
			Ajax.post(contextUrl + "/tasks/submitLoad", null, $loadForm);
		});
		
		//-------------------------------------------------------
		//sort tab
		//-------------------------------------------------------
		var $sortForm = $tabs.find("form#workbench-sortForm");
		
		$sortForm.find("select, input").change(function() {
			Ajax.post(contextUrl + "/tasks/submitSort", null, $sortForm)
				.then(taskList.update);
		});
		
		//-------------------------------------------------------
		//search tab
		//-------------------------------------------------------
		var $searchForm = $tabs.find("form#workbench-searchForm");
		
		$searchForm.find(".workbench-search-submit").click(function() {
			Ajax.post(contextUrl + "/tasks/submitSearch", null, $searchForm)
				.then(taskList.update);
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
		(function() {
			var tabId = "filter";
			var $tab = tabIdsToMenuTabs[tabId].$tab;
			var get$FilterForm = function() {
				return $tab.find("form#generalFilters");
			};
			var allSelector = "#generalFilters-all";
			var get$All = function() {
				return $tab.find(allSelector);
			};
			
			// ajax
			var submitFilterForm = function(reset) {
				var data = { reset: reset ? true : false };
				Ajax.post(contextUrl + "/tasks/filter", data, get$FilterForm())
					.then(onSubmitAnswer);
			};
			var getFilterForm = function() {
				Ajax.get(contextUrl + "/tasks/filter", {})
					.then(onSubmitAnswer);
			};
			var onSubmitAnswer = function(answer) {
				var isDefault = answer.isInDefaultState;
				highlightMenuTab(tabId, isDefault === "false", function() {
					// reset
					submitFilterForm(true);
				});
				var html = answer.html;
				if (html !== undefined) {
					get$FilterForm().remove();
					$tab.append(html);
				}
				taskList.update();
			};
			
			// bind checkboxes
			$tab.on("change", ".generalFilters-notarget", function() {
				submitFilterForm();
			});
			var cbg = new CheckboxGrouper($tab, ".generalFilters-all-target", function(areChecked) {
				get$All().prop("checked", areChecked);
				submitFilterForm();
			});
			$tab.on("change", allSelector, function() {
				var isChecked = get$All().prop("checked");
				cbg.changeGroup(isChecked);
				submitFilterForm();
			});
			
			// get initial filterForm html
			getFilterForm();
		})();
		
		//-------------------------------------------------------
		//admin tab
		//-------------------------------------------------------
		var $saveTasks = $("#dialog-saveTasks");
		var $saveTasksForm = $saveTasks.find("#dialog-saveTasks-form");
		
		$saveTasks.find("#dialog-saveTasks-saveButton").click(function() {
			Ajax.post(contextUrl + "/tasks/workbench/admin/save", {}, $saveTasksForm)
				.then(function() {
					Dialogs.hideDialog($("#dialog-saveTasks"));
				});
		});
		$tabs.find("#workbench-admin-saveButton").click(function() {
			Dialogs.showDialog($saveTasks);
		});
		$tabs.find("#workbench-admin-deleteButton").click(function() {
			var $confirm = Dialogs.confirm(function() {
				Dialogs.hideDialog($confirm);
				Ajax.post(contextUrl + "/tasks/workbench/admin/delete")
					.then(function(){
						taskList.update();
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
		var taskForm = TaskForm();
		
		var workbench = new Workbench(taskForm);
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
 * @param {jquery} $checkboxGroup - all checkboxes of the controlled group (static).
 * @param {string} [selector] - all checkboxes of the controlled group (dynamic as in
 * 		jquery.on($checkboxGroup, selector, callback).
 * @param {function} onGroupChange - gets called on any change with boolean argument: if true, all
 * 		checkboxes are checked, if false at least one is unchecked (used to update controller).
 */
function CheckboxGrouper($checkboxGroup, selector, onGroupChange) {
	if (onGroupChange === undefined) {
		onGroupChange = selector;
		selector = null;
	}
	$checkboxGroup.on("change", selector, function($element) {
		if(!$checkboxGroup.findSelf(selector).is(':not(:checked)')) {
			onGroupChange(true);
		}
		else {
			onGroupChange(false);
		}
	});
	
	/**
	 * Call this when controller wants to un-/check the group.
	 * @param isChecked - true if controller wants to check, false if uncheck
	 */
	this.changeGroup = function(isChecked) {
		$checkboxGroup.findSelf(selector).prop("checked", isChecked);
	};
}
