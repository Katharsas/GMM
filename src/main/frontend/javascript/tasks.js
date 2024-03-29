import $ from "./lib/jquery";
import Ajax from "./shared/ajax";
import Dialogs from "./shared/dialogs";
import TaskForm from "./shared/TaskForm";
import { DataChangeNotifierInit } from "./shared/DataChangeNotifier";
import TaskCache from "./tasks/TaskCache";
import TaskSwitcher from "./tasks/TaskSwitcher";
import PinnedList from "./tasks/PinnedList";
import WorkbenchList from "./tasks/WorkbenchList";
import TaskEventBindings from "./tasks/TaskEventBindings";
import { TaskDialogsInit } from "./shared/TaskDialog";
import Notifications from "./shared/notifications";
import { contextUrl, getURLParameter, allVars } from "./shared/default";
import HtmlPreProcessor from "./shared/preprocessor";
import {} from "./shared/template";

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
var Workbench = function(taskCache, taskSwitcher, taskBinders) {
	
	var $workbench = $("#workbench");
	var $workbenchList = $workbench.find(".list-body");
	var $tabs = $workbench.find("#workbench-tabs .workbench-tab");
	var tabIdsToMenuTabs = {};
	
	var $count = $workbenchList.find(".list-count span");
	
	var taskListSettings = {
		taskListId : "workbench",
		$list : $workbenchList,
		eventUrl : "/workbench/taskListEvents",
		initUrl : "/workbench/init",
		eventBinders : taskBinders,
		onUpdateStart : function() {
			$count.text("...");
		},
		onUpdateDone : function(newSize) {
			$count.text(newSize);
		},
		currentUser : allVars.currentUser,
		expandSingleTask : true
	};
	var taskList = WorkbenchList(taskListSettings, taskCache, taskSwitcher);
	
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
		(function() {
			const tabId = "load";
			const $tab = tabIdsToMenuTabs[tabId].$tab;
			let $loadForm;
			let $loadButtons;

			var updateLoadedButtons = function() {
				Ajax.get(contextUrl + "/workbench/selected")
				.then(function(selected) {
					$loadButtons.each(function(index, element) {
						if (selected[index]) {
							$(element).addClass("selected");
						} else {
							$(element).removeClass("selected");
						}
					});
				});
			}

			Ajax.get(contextUrl + "/workbench/load")
			.then(function(answer){
				var html = answer.html;
				$tab.append(html);
				HtmlPreProcessor.apply($tab);

				$loadForm = $tab.find("form#workbench-loadForm");
				$loadButtons = $tab.find(".workbench-load-typeButton");
				
				updateLoadedButtons();

				$loadButtons.click(function() {
					var type = $(this).data("type");
					Ajax.post(contextUrl + "/workbench/loadType", { type: type })
						.then(function() {
							updateLoadedButtons();
							taskList.update();
						});
				});

				$loadForm.find(".form-element").change(function() {
					Ajax.post(contextUrl + "/workbench/loadOptions", null, $loadForm);
				});
			});
		})();
		
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
			$tab.on("keypress", "input.form-element", function(event) {
				if (event.which === 13) {
					submitSearchForm(false);
				}
			})
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
					HtmlPreProcessor.apply($tab);
				}
				$searchForm = $tab.find("form#workbench-searchForm");
				$searchType = $searchForm.find("select#workbench-search-type");
				setSearchType(isEasySearch());

				$searchForm.find("input").on('paste', function(e) {
					const $that = $(this);
					setTimeout(function () {
						$that.val($that.val().trim());
					}, 50);
				});
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
					HtmlPreProcessor.apply($tab);
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

	return taskList;
};

var PinnedTasks = function(taskCache, taskSwitcher, taskBinders) {
	
	var $pinned = $("#pinned");
	var $pinnedList = $pinned.find(".list-body");
	
	var taskListSettings = {
		taskListId : "pinnedTasks",
		$list : $pinnedList,
		eventUrl : "/tasks/pinned/taskListEvents",
		initUrl : "/tasks/pinned/init",
		eventBinders : taskBinders,
		onUpdateDone : function(newSize) {
			$pinned.toggle(newSize > 0);
		},
		currentUser : allVars.currentUser
	};
	var taskList = PinnedList(taskListSettings, taskCache, taskSwitcher);
	return taskList.update;
};

/**
 * This function is executed when document is ready for interactivity!
 */
$(document).ready(
	function() {
		tasksVars.edit = getURLParameter("edit");
		
		DataChangeNotifierInit("/tasks/taskDataEvents");

		var taskForm = TaskForm();
		var taskCache = TaskCache("/tasks/renderTaskData");
		var taskBinders = TaskEventBindings(
			taskForm.prepareEdit, taskCache.triggerIsPinned, taskCache.isPinned);
		var taskSwitcher = TaskSwitcher();
		
		var pinnedList = new PinnedTasks(taskCache, taskSwitcher, taskBinders);
		var workbenchList = new Workbench(taskCache, taskSwitcher, taskBinders);

		TaskDialogsInit(taskCache, taskBinders);
		//TaskDialogs.openDialog("GeneralTask133");
		// TaskDialogs.openDialog("GeneralTask155");

		//TODO sidebarmarker creation on task select
//			SidebarMarkers = SidebarMarkers(function() {
//				return $('<div>').html("Marker");
//			}, 2);
//			SidebarMarkers.registerSidebar("#page-tabmenu-spacer", true);
//			SidebarMarkers.addMarker("#test1");
//			SidebarMarkers.addMarker("#test2");
		
		Notifications.init();
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
