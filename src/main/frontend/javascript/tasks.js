import $ from "./lib/jquery";
import Ajax from "./shared/ajax";
import Dialogs from "./shared/dialogs";
import Queue from "./shared/queue";
import TaskLoader from "./shared/taskloader";
import TaskSwitcher from "./shared/taskswitcher";
import TaskEventBindings from "./shared/tasklisteners";
import ResponseBundleHandler from "./shared/responseBundleHandler";
import { contextUrl, allVars, getURLParameter } from "./shared/default";

var tasksVars = {
	"edit" : "",
};

var tasksFuncs = {
	"refresh" : function() {
		var url = contextUrl + "/tasks";
		if (tasksVars.edit !== "") {
			url += "?edit=" + tasksVars.edit;
		}
		window.location.href =  url;
	}
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
	var expandedTasks = new Queue(3, function($task1, $task2) {
		return $task1[0] === $task2[0];
	});
	
	var taskBinders = TaskEventBindings(
		function($task) {
			taskSwitcher.switchTask($task, expandedTasks);
		},
		function($task, id) {
			taskLoader.updateTask(taskListId, id);
		},
		function($task, id) {
			taskLoader.removeTask(id);
			taskForm.resetFormIfUnderEdit(id);
		},
		function(id) {
			taskForm.prepareEdit(id);
		}
	);
	taskLoader.setTaskEventBinders(taskListId, taskBinders);
	taskForm.setOnEdit(function(id) {
		taskLoader.updateTask(taskListId, id);
	});
	taskForm.setOnCreate(function() {
		render();
	});
	
	var render = function() {
		taskLoader.createTaskList(taskListId, function() {
			$count.text(taskLoader.getTaskIds.length);
			expandedTasks.clear();
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
			var $confirm = Dialogs.confirm(function() {
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
 * Singleton, since there can only be one taskForm perpage currently.
 * Initializes task form and registers behaviour for task form buttons.
 */
var TaskForm = (function() {
	var instance = null;
	var TaskForm = function() {
		
		// null if there is no editing going on, idLink otherwise
		var currentlyEditedId = null;
		var onEdit = null;
		var onCreate = null;
		
		var $form = $("#taskForm");
		var $new = $("#newTaskButton");
		var $cancel = $("#cancelTaskButton");
		var $submit = $("#submitTaskButton");
		
		$new.on("click", function() {
			show();
		});
		$submit.on("click", function() {
			if(currentlyEditedId !== null) {
				// submit edit
				Ajax.post(contextUrl + "/tasks/editTask/submit", null, $form)
					.done(function() {
						var idBuffer = currentlyEditedId;
						resetTaskForm();
						onEdit(idBuffer);
					});
			} else {
				// submit new task
				var url = contextUrl + "/tasks/createTask";
				var ajaxChannel = new ResponseBundleHandler(url, "assets", true);
				ajaxChannel.start({$taskForm: $("#taskForm")}, function() {
					resetTaskForm();
					onCreate();
				});
			}
			// TODO refresh added /edited task from server
		});
		$cancel.on("click", function() {
			resetTaskForm();
		});
		resetTaskForm();
		
		// TODO show user if he is currently editing or creating new task
		
		function resetTaskForm() {
			Ajax.post(contextUrl + "/tasks/resetTaskForm")
				.done(function() {
					hide();
					getAndInsertForm();
				});
		}
		
		function getAndInsertForm() {
			$form.empty();
			Ajax.get(contextUrl + "/tasks/renderTaskForm")
				.done(function(data) {
					$form.html(data.taskFormHtml);
					currentlyEditedId = data.editedTaskIdLink;
					if(currentlyEditedId !== null) {
						// if editing, hide type selection
						var $type = $form.find("#taskForm-group-type");
						$type.hide();
					} else {
						// else show path if asset
						var $typeSelect = $form.find("#taskForm-element-type select");
						$typeSelect.on("change", function() {switchAssetPath($typeSelect);});
						switchAssetPath($typeSelect);
					}
				}
			);
		}
		
		function switchAssetPath($taskElementType) {
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
		
		function hide() {
			$form.hide();
			$submit.hide();
			$cancel.hide();
			$new.show();
		}
		
		function prepareEdit(id) {
			Ajax.post(contextUrl + "/tasks/editTask/announce", {idLink : id})
				.done(function() {
					getAndInsertForm();
					show();
				});
		}
		
		function resetIfEdited(id) {
			if(currentlyEditedId == id) {
				resetTaskForm();
			}
		}
		
		/**
		 * @callback onEditCallback - called with edited tasks id on edit submit
		 */
		function setOnEdit(onEditCallback) {
			onEdit = onEditCallback;
		}
		/**
		 * @callback onCreateCallback - called on create task
		 */
		function setOnCreate(onCreateCallback) {
			onCreate = onCreateCallback;
		}
		
		return {
			prepareEdit : prepareEdit,
			resetFormIfUnderEdit : resetIfEdited,
			setOnEdit : setOnEdit,
			setOnCreate : setOnCreate
		};
	};
	return function() {
		if(instance === null) instance = TaskForm();
		return instance;
	};
})();