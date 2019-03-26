import $ from "../lib/jquery";
import Ajax from "./ajax";
import { contextUrl } from "./default";
import ResponseBundleHandler from "./responseBundleHandler";

/**
 * -------------------- TaskForm -----------------------------------------------------------------
 * Singleton, since there can only be one taskForm per page currently.
 * Initializes task form and registers behaviour for task form buttons.
 */
var TaskForm = (function() {
	var instance = null;
	var TaskForm = function() {
		
		// null if there is no editing going on, idLink otherwise
		var currentlyEditedId = null;
		var onEdit = [];
		var onCreate = [];
		
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
				.then(function() {
					resetTaskForm();
					for (let callback of onEdit) {
						callback();
					}
				});
			} else {
				// submit new task
				var url = contextUrl + "/tasks/createTask";
				var ajaxChannel = new ResponseBundleHandler(url, "assets", true);
				ajaxChannel.start({$taskForm: $("#taskForm")}, function() {
					resetTaskForm();
					for (let callback of onCreate) {
						callback();
					}
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
			.then(function() {
				hide();
				getAndInsertForm();
			});
		}
		
		function getAndInsertForm() {
			$form.empty();
			Ajax.get(contextUrl + "/tasks/renderTaskForm")
			.then(function(data) {
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
			.then(function() {
				getAndInsertForm();
				show();
				$(window).scrollTop(0);
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
		function registerOnEdit(onEditCallback) {
			onEdit.push(onEditCallback);
		}
		/**
		 * @callback onCreateCallback - called on create task
		 */
		function registerOnCreate(onCreateCallback) {
			onCreate.push(onCreateCallback);
		}
		
		return {
			prepareEdit : prepareEdit,
			resetFormIfUnderEdit : resetIfEdited,
			registerOnEdit : registerOnEdit,
			registerOnCreate : registerOnCreate
		};
	};
	return function() {
		if(instance === null) instance = TaskForm();
		return instance;
	};
})();

export default TaskForm;