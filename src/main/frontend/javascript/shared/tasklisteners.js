import $ from "../lib/jquery";
import Ajax from "./ajax";
import Dialogs from "./dialogs";
import PreviewRenderer from "./PreviewRenderer";
import { contextUrl, allVars, htmlDecode } from "./default";

/**
 * ------------- TaskEventBindings ---------------------------------------------
 * Provides methods to bind all needed listeners to task header or body
 * 
 */
export default function(onedit) {
	
	//if(1 == "text") console.log(x = 10);
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
	
	var changeComment = function(comment, taskId, commentId, $task, onchange) {
		var $confirm = Dialogs.confirm(
			function(input, textarea) {
				var url = contextUrl + "/tasks/editComment/" + taskId + "/" + commentId;
				Ajax.post(url, {"editedComment" : textarea}) 
					.done(function() {
						Dialogs.hideDialog($confirm);
						onchange($task, taskId);
					});
			}, "Change your comment below:", undefined, comment, 700);
	};
	
	//download
	
	var downloadFromPreview = function(taskId, version) {
		var uri = contextUrl + "/tasks/download/" + taskId + "/preview/" + version + "/";
		window.open(uri);
	};
	
	// file trees
	
	var selectedFileIsAsset;
	var $selectedFile = $();
	
	var subDir = function() {
		return selectedFileIsAsset ? "asset" : "other";
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
		
		bindList : function($list, onswitch) {
			$list.on("click", ".task-header", function() {
				onswitch($(this).parent(".task"));
			});
		},
		
		bindHeader : function($task) {
		},
		
		/**
		 * @param $body - Complete body part of a task, bind functions to this (or its children).
		 * @param $task - Will point to the complete task in the future. Use as callback parameter only.
		 */
		bindBody : function(id, $task, $body, onchange) {
			
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
						.done(function() {
							Dialogs.hideDialog($confirm);
							onchange($task, id);
						});
				}, "Are you sure you want to delete this task?");
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
				var $text = $comment.children(".task-comment-text");
				changeComment(htmlDecode($text.html()), id, $comment.attr("id"), $task, onchange);
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
					.done(function() {
						onchange($task, id);
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
					downloadFromPreview(id, 'original');
				});
				$preview.find(".task-preview-button-newest").click(function() {
					downloadFromPreview(id, 'newest');
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
			
			var $files = $body.find(".task-files");
			if ($files.length > 0) {
				
				//file trees
				var fileTreeOptions = function(isAsset) {
					return {
						url: contextUrl + "/tasks/files/" + isAsset.toString() + "/" + id,
						directoryClickable: false
					};
				};
				var $fileTreeAssets = $files.find(".task-files-assets-tree");
				$fileTreeAssets.fileTree(fileTreeOptions(true),
					function($file) {
						selectFile($file, true);
					}
				);
				var $fileTreeOther = $files.find(".task-files-other-tree");
				$fileTreeOther.fileTree(fileTreeOptions(false),
					function($file) {
						selectFile($file, false);
					}
				);
				
				var $fileOps = $files.find(".task-files-operations");
				
				//upload
				var $inputFile = $fileOps.find(".task-files-uploadInput");
				//upload when a file is chosen (on hidden input tag)
				$inputFile.change(function() {
					allVars.$overlay.show();
					var file = $inputFile[0].files[0];
					Ajax.upload(contextUrl + "/tasks/upload/" + id, file)
						.done(function() {
							//TODO refresh filetree only
							Dialogs.alert(function(){onchange($task, id);}, "TODO: Refresh filetree only");
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
					var uri = contextUrl + "/tasks/download/" + id + "/" + subDir() + "/" + dir + "/";
					window.open(uri);
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
							.done(function() {
								Dialogs.hideDialog($dialog);
								//TODO refresh filetree only
								Dialogs.alert(function(){onchange($task, id);}, "TODO: Refresh filetree only");
							});
					}, "Delete " + filePath() + " ?");
				});
			}
		}
	};
}