import $ from "../lib/jquery";
import Ajax from "../shared/ajax";
import Dialogs from "../shared/dialogs";
import PreviewRenderer from "../shared/PreviewRenderer";
import { contextUrl, htmlDecode } from "../shared/default";

/**
 * ------------- TaskEventBindings ---------------------------------------------
 * Provides methods to bind all needed listeners to task header or body
 * 
 */
export default function(onedit) {
	
	var onTaskListChangeCallbacks = [];
	var onPinnedChange;
	
	var updateTaskLists = function() {
		for (let callback of onTaskListChangeCallbacks) {
			callback();
		}
	};
	var updatePinnedList = function() {
		if (onPinnedChange !== undefined) {
			onPinnedChange();
		}
	};
	
	//comments
	var hideCommentForm = function($commentForm) {
		var $elementComments = $commentForm.parent();
		if ($elementComments.is(":visible:blank")) {
			$elementComments.hide();
		}
		$commentForm.hide();
	};
	
	var showCommentForm = function ($commentForm) {
		$commentForm.parent().show();
		$commentForm.show();
	};
	
	//download
	var downloadAssetFile = function(taskId, groupType) {
		var uri = contextUrl + "/tasks/download/" + taskId + "/" + groupType + "/ASSET/";
		window.open(uri);
	};
	
	var downloadOtherFile = function(taskId, fileType, dir) {
		var uri = contextUrl + "/tasks/download/" + taskId + "/NEW/" + fileType + "/" + dir + "/";
		window.open(uri);
	};
	
	// file trees
	var selectedFileIsAsset;
	var $selectedFile = $();
	
	var fileType = function() {
		return selectedFileIsAsset ? "ASSET" : "WIP";
	};
	var filePath = function() {
		return $selectedFile.attr("rel");
	};
	var selectFile = function($file, isAsset) {
		selectedFileIsAsset = isAsset;
		$selectedFile.removeClass("task-files-selected");
		$selectedFile = $file;
		$selectedFile.addClass("task-files-selected");
	};
	
	
	return {
		
		setOnPinnedChange : function(callback) {
			onPinnedChange = callback;
		},
		
		bindList : function($list, onswitch, onchange) {
			$list.on("click", ".task-header", function() {
				onswitch($(this).parent(".task"));
			});
			onTaskListChangeCallbacks.push(onchange);
		},
		
		bindHeader : function($task) {},
		
		/**
		 * @param $body - Complete body part of a task, bind functions to this (or its children).
		 * @param $task - Will point to the complete task in the future. Use as callback parameter only.
		 * @param {callback} updateTaskList - Function which returns a promise to finish updating.
		 */
		bindBody : function(id, $task, $body) {
			
			/* -------------------------------------------------------
			 * GENERAL TASK
			 * -------------------------------------------------------
			 */
			
			var $operations = $body.find(".task-body-footer").children(".task-operations");
			//edit task
			$operations.find(".task-operations-editTask").click(function() {
				onedit(id);
			});
			//delete task
			$operations.find(".task-operations-deleteTask").click(function() {
				var $confirm = Dialogs.confirm(function() {
					Ajax.post(contextUrl + "/tasks/deleteTask/" + id)
					.then(function() {
						Dialogs.hideDialog($confirm);
						updateTaskLists();
					});
				}, "Are you sure you want to delete this task?");
			});
			//pin task
			$operations.find(".task-operations-pin").click(function() {
				Ajax.post(contextUrl + "/tasks/pinned/pin", { idLink: id })
				.then(function() {
					updatePinnedList();
				});
			});
			// unpin task
			$operations.find(".task-operations-unpin").click(function() {
				Ajax.post(contextUrl + "/tasks/pinned/unpin", { idLink: id })
				.then(function() {
					updatePinnedList();
				});
			});
			
			/* -------------------------------------------------------
			 * GENERAL TASK - COMMENTS
			 * -------------------------------------------------------
			 */
			
			//comments
			var $comments = $body.find(".task-comments");
			var $form = $comments.children("form.task-comments-form");
			//show comment edit dialog
			$comments.on("click", ".task-comment-editButton", function() {
				var $comment = $(this).parent(".task-comment");
				var comment = htmlDecode($comment.children(".task-comment-text").html());
				var commentId = $comment.attr("id");
				var $confirm = Dialogs.confirm(
					function(input, textarea) {
						var url = contextUrl + "/tasks/editComment/" + id + "/" + commentId;
						Ajax.post(url, {"editedComment" : textarea}) 
							.then(function() {
								Dialogs.hideDialog($confirm);
								updateTaskLists();
							});
					}, "Change your comment below:", undefined, comment, 700);
			});
			//show/hide new comment form
			$operations.find(".task-operations-switchComment").click(function() {
				if ($form.is(":visible")) hideCommentForm($form);
				else showCommentForm($form);
			});
			//submit new comment
			$form.find(".task-comment-form-submitButton").click(function() {
				var url = contextUrl + "/tasks/submitComment/" + id;
				Ajax.post(url, {}, $form)
					.then(function() {
						updateTaskLists();
					});
			});
			
			/* -------------------------------------------------------
			 * ASSET TASK - PREVIEW
			 * -------------------------------------------------------
			 */
			
			var $preview = $body.find(".task-preview");
			if($preview.length > 0)
			{
				//download from preview
				$preview.find(".task-preview-button-original").click(function() {
					downloadAssetFile(id, 'ORIGINAL');
				});
				$preview.find(".task-preview-button-newest").click(function() {
					downloadAssetFile(id, 'NEW');
				});
				
				//3D preview
				var $canvasContainer = $preview.find(".task-preview-visuals.task-preview-3D");
				if($canvasContainer.length > 0) {
					var renderer = PreviewRenderer($canvasContainer);
					
					var $renderOptions = $preview.find(".task-preview-renderOptions");
					
					var $renderSolid = $renderOptions.find(".renderOption-solid");
					var $renderWire = $renderOptions.find(".renderOption-wire");
					// render mode
					$renderOptions.find(".renderOption-solid").on("click", function() {
						$renderWire.removeClass("active");
						$renderSolid.addClass("active");
						renderer.setOptions({showWireframe: false});
					});
					$renderOptions.find(".renderOption-wire").on("click", function() {
						$renderSolid.removeClass("active");
						$renderWire.addClass("active");
						renderer.setOptions({showWireframe: true});
					});
					// checkboxes
					var $shadows = $renderOptions.find(".renderOption-shadows input");
					$shadows.prop('checked', renderer.getOption("shadowsEnabled"));
					$shadows.on("change", function() {
						renderer.setOptions({shadowsEnabled: $(this).is(":checked")});
					});
					var $rotLight = $renderOptions.find(".renderOption-rotLight input");
					$rotLight.prop('checked', renderer.getOption("rotateLight"));
					$rotLight.on("change", function() {
						renderer.setOptions({rotateLight: $(this).is(":checked")});
					});
					var $rotCamera = $renderOptions.find(".renderOption-rotCamera input");
					$rotCamera.prop('checked', renderer.getOption("rotateCamera"));
					$rotCamera.on("change", function() {
						renderer.setOptions({rotateCamera: $(this).is(":checked")});
					});
					// number input
					var $rotCameraSpeed = $renderOptions.find(".renderOption-rotCameraSpeed");
					$rotCameraSpeed.val(renderer.getOption("rotateCameraSpeed"));
					$rotCameraSpeed.on("input", function() {
						var speed = parseFloat($rotCameraSpeed.val());
						if (!isNaN(speed) && speed < 100) {
							renderer.setOptions({rotateCameraSpeed: speed});
						}
					});
				}
			}
			
			/* -------------------------------------------------------
			 * ASSET TASK - FILES
			 * -------------------------------------------------------
			 */
			
			// TODO file tree changes need to be events received by all task lists.
			// (instead of just rebuilding the filetree for the current task)
			
			var $files = $body.find(".task-files");
			if ($files.length > 0) {
				
				//file trees
				var fileTreeOptions = function(isAsset) {
					return {
						url: contextUrl + "/tasks/files/" + isAsset.toString() + "/" + id,
						directoryClickable: false
					};
				};
//				var $fileTreeAssets = $files.find(".task-files-assets-tree");
				var $fileTreeOther = $files.find(".task-files-wip-tree");
				var createFileTrees = function() {
//					$fileTreeAssets.fileTree(fileTreeOptions(true),
//						function($file) {
//							selectFile($file, true);
//						}
//					);
					$fileTreeOther.fileTree(fileTreeOptions(false),
						function($file) {
							selectFile($file, false);
						}
					);
				};
				createFileTrees();
				
				var $fileOps = $files.find(".task-files-operations");
				
				//upload
				var $inputFile = $fileOps.find(".task-files-uploadInput");
				//upload when a file is chosen (on hidden input tag)
				$inputFile.change(function() {
					Dialogs.showOverlay();
					var file = $inputFile[0].files[0];
					Ajax.upload(contextUrl + "/tasks/upload/" + id, file)
						.then(function() {
							Dialogs.hideOverlay();
							createFileTrees();
						});
				});
				//bind triggering of filechooser to button
				$fileOps.find(".task-files-button-upload").click(function() {
					$inputFile.click();
				});
				
				//download
				$fileOps.find(".task-files-button-download").click(function() {
					var dir = filePath();
					if (dir === undefined || dir === "") return;
					downloadOtherFile(id, fileType(), dir);
				});
				
				//delete
				$fileOps.find(".task-files-button-delete").click(function() {
					var dir = filePath();
					if (dir === undefined || dir === "") {
						return;
					}
					var $dialog = Dialogs.confirm(function() {
						Ajax.post(contextUrl + "/tasks/deleteFile/" + id,
								{dir: dir, asset: selectedFileIsAsset.toString()})
							.then(function() {
								Dialogs.hideDialog($dialog);
								createFileTrees();
							});
					}, "Delete " + filePath() + " ?");
				});
			}
		}
	};
}