import $ from "./lib/jquery";
import Ajax from "./shared/ajax";
import Dialogs from "./shared/dialogs";
import TaskForm from "./shared/TaskForm";
import TaskCache from "./tasklist/TaskCache";
import TaskList from "./tasklist/TaskList";
import TaskSwitcher from "./tasklist/TaskSwitcher";
import TaskEventBindings from "./tasklist/TaskEventBindings";
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
var Workbench = function(taskCache, taskForm, taskSwitcher) {
	
	var $workbench = $("#workbench");
	var $workbenchList = $workbench.find(".list-body");
	var $tabs = $workbench.find("#workbench-tabs .workbench-tab");
	var tabIdsToMenuTabs = {};
	
	var $loadButtons = $tabs.find(".workbench-load-typeButton");
	var $count = $workbenchList.find(".list-count span");
	
	var taskBinders = TaskEventBindings(taskForm.prepareEdit);
	
	var taskListSettings = {
		taskListId : "workbench",
		$list : $workbenchList,
		eventUrl : "/workbench/taskListEvents",
		eventBinders : taskBinders,
		onChange : function(newSize) {
			$count.text(newSize);
		}
	};
	var taskList = TaskList(taskListSettings, taskCache, taskSwitcher);

	taskForm.setOnEdit(function(id) {
		taskList.markTaskDeprecated(null, id);
	});
	taskForm.setOnCreate(taskList.update);
	
	var updateTasks = function() {
		Ajax.get(contextUrl + "/workbench/selected")
			.then(function(selected) {
				$loadButtons.each(function(index, element) {
					if (selected[index]) {
						$(element).addClass("selected");
					} else {
						$(element).removeClass("selected");
					}
				});
				taskList.update();
			});
	};
	this.load = function(type) {
		Ajax.post(contextUrl + "/workbench/loadType", { type: type })
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
			Ajax.post(contextUrl + "/workbench/loadOptions", null, $loadForm);
		});
		
		//-------------------------------------------------------
		//sort tab
		//-------------------------------------------------------
		var $sortForm = $tabs.find("form#workbench-sortForm");
		
		$sortForm.find("select, input").change(function() {
			Ajax.post(contextUrl + "/workbench/sort", null, $sortForm)
				.then(taskList.update);
		});
		
		//-------------------------------------------------------
		//search tab
		//-------------------------------------------------------
		(function() {
			var tabId = "search";
			var $tab = tabIdsToMenuTabs[tabId].$tab;
			var $searchForm;
			var $searchType;
			
			$tab.on("click", ".workbench-search-submit", function(){
				submitSearchForm(false);
			});
			$tab.on("click", "#workbench-search-switch", function(){
				setSearchType(!isEasySearch());
			});
			var submitSearchForm = function(reset) {
				var data = { reset: reset ? true : false };
				Ajax.post(contextUrl + "/workbench/search", data, $searchForm)
				.then(function(answer) {
					onSubmitAnswer(answer);
					taskList.update();
				});
			};
			
			function isEasySearch() {
				return $searchType.val() === "true";
			}
			function setSearchType(isEasySearch) {
				var $easy = $searchForm.find("#workbench-search-easy");
				var $complex = $searchForm.find("#workbench-search-complex");
				$searchType.val(isEasySearch.toString());
				$easy.toggle(isEasySearch);
				$complex.toggle(!isEasySearch);
			}
			var onSubmitAnswer = function(answer) {
				var isDefault = answer.isInDefaultState;
				highlightMenuTab(tabId, isDefault === "false", function() {
					// reset
					submitSearchForm(true);
				});
				var html = answer.html;
				if (html !== undefined) {
					$tab.children().remove();
					$tab.append(html);
				}
				$searchForm = $tab.find("form#workbench-searchForm");
				$searchType = $searchForm.find("select#workbench-search-type");
				setSearchType(isEasySearch());
			};
			
			//get initial searchForm
			Ajax.get(contextUrl + "/workbench/search", {})
			.then(onSubmitAnswer);
		})();
		
		//-------------------------------------------------------
		//filter tab
		//-------------------------------------------------------
		(function() {
			var tabId = "filter";
			var $tab = tabIdsToMenuTabs[tabId].$tab;
			var $filterForm;
			var $all;
			var allSelector = "#generalFilters-all";
			
			// ajax
			var submitFilterForm = function(reset) {
				var data = { reset: reset ? true : false };
				Ajax.post(contextUrl + "/workbench/filter", data, $filterForm)
				.then(function(answer){
					onSubmitAnswer(answer);
					taskList.update();
				});
			};
			var onSubmitAnswer = function(answer) {
				var isDefault = answer.isInDefaultState;
				highlightMenuTab(tabId, isDefault === "false", function() {
					// reset
					submitFilterForm(true);
				});
				var html = answer.html;
				if (html !== undefined) {
					$tab.children().remove();
					$tab.append(html);
				}
				$filterForm = $tab.find("form#generalFilters");
				$all = $tab.find(allSelector);
			};
			
			// bind checkboxes
			$tab.on("change", ".generalFilters-notarget", function() {
				submitFilterForm();
			});
			var cbg = new CheckboxGrouper($tab, ".generalFilters-all-target", function(areChecked) {
				$all.prop("checked", areChecked);
				submitFilterForm();
			});
			$tab.on("change", allSelector, function() {
				var isChecked = $all.prop("checked");
				cbg.changeGroup(isChecked);
				submitFilterForm();
			});
			
			// get initial filterForm html
			Ajax.get(contextUrl + "/workbench/filter", {})
			.then(onSubmitAnswer);
		})();
		
		//-------------------------------------------------------
		//admin tab
		//-------------------------------------------------------
		var $saveTasks = $("#dialog-saveTasks");
		var $saveTasksForm = $saveTasks.find("#dialog-saveTasks-form");
		
		$saveTasks.find("#dialog-saveTasks-saveButton").click(function() {
			Ajax.post(contextUrl + "/workbench/saveVisible", {}, $saveTasksForm)
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
				Ajax.post(contextUrl + "/workbench/deleteVisible")
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

var PinnedTasks = function(taskCache, taskForm, taskSwitcher) {
	
	var $pinned = $("#pinned");
	var $pinnedList = $pinned.find(".list-body");
	var taskBinders = TaskEventBindings(taskForm.prepareEdit);
	
	var taskListSettings = {
		taskListId : "pinnedTasks",
		$list : $pinnedList,
		eventUrl : "/tasks/pinned/taskListEvents",
		eventBinders : taskBinders,
		onChange : function(newSize) {
			$pinned.toggle(newSize > 0);
		}
	};
	var taskList = TaskList(taskListSettings, taskCache, taskSwitcher);
	
	// TODO create function registerTaskList on TaskForm which allows callbacks for multiple lists
//	taskForm.setOnEdit(function(id) {
//		taskList.markTaskDeprecated(null, id);
//	});
//	taskForm.setOnCreate(taskList.update);
	
	taskList.update();
};

/**
 * This function is executed when document is ready for interactivity!
 */
$(document).ready(
	function() {
		tasksVars.edit = getURLParameter("edit");
		
		var taskForm = TaskForm();
		var taskCache =  TaskCache("/tasks/renderTaskData");
		var taskSwitcher = TaskSwitcher();
		
		var workbench = new Workbench(taskCache, taskForm, taskSwitcher);
		global.workbench = workbench;
		
		new PinnedTasks(taskCache, taskForm, taskSwitcher);
		
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
