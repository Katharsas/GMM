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
export default function(onswitch, onchange, onremove, onedit) {
	
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
	
	var changeComment = function(comment, taskId, commentId, $task, id) {
		var $confirm = Dialogs.confirm(
			function(input, textarea) {
				var url = contextUrl + "/tasks/editComment/" + taskId + "/" + commentId;
				Ajax.post(url, {"editedComment" : textarea}) 
					.done(function() {
						Dialogs.hideDialog($confirm);
						onchange($task, id);
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
		
		//TODO bind to list instead of each task
		bindHeader : function($task) {
			$task.children(".task-header").click(function() {
				onswitch($(this).parent(".task"));
			});
		},
		
		/**
		 * @param $body - Complete body part of a task, bind functions to this (or its children).
		 * @param $task - Will point to the complete task in the future. Use as callback parameter only.
		 */
		bindBody : function(id, $task, $body) {
			
			/* -------------------------------------------------------
			 * GENERAL TASK
			 * -------------------------------------------------------
			 */
			
			//comments
			var $comments = $body.find(".task-comments");
			var $form = $comments.children("form.task-comments-form");
			var $operations = $body.find(".task-body-footer").children(".task-operations");
			//show comment edit dialog
			$comments.on("click", ".task-comment-editButton", function() {
				var $comment = $(this).parent(".task-comment");
				var $text = $comment.children(".task-comment-text");
				changeComment(htmlDecode($text.html()), id, $comment.attr("id"), $task, id);
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
							onremove($task, id);
						});
				}, "Are you sure you want to delete this task?");
			});
			
			/* -------------------------------------------------------
			 * ASSET TASK
			 * -------------------------------------------------------
			 */
			
			var $preview = $body.find(".task-preview");
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
				
				//download from preview
				$preview.find(".task-preview-button-original").click(function() {
					downloadFromPreview(id, 'original');
				});
				$preview.find(".task-preview-button-newest").click(function() {
					downloadFromPreview(id, 'newest');
				});
				
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
			
			/* -------------------------------------------------------
			 * 3D PREVIEW
			 * -------------------------------------------------------
			 */
			
			var $canvasContainer = $preview.find(".task-preview-visuals");
			var $renderOptions = $preview.find(".task-preview-renderOptions");

			//TODO
			if($renderOptions.length > 0) {
				var renderer = PreviewRenderer($canvasContainer);
			}
		}
	};
}