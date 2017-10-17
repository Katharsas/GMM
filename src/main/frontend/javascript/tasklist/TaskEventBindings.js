import $ from "../lib/jquery";
import Ajax from "../shared/ajax";
import Dialogs from "../shared/dialogs";
import PreviewRenderer from "../shared/PreviewRenderer";
import { ShadingType } from "../shared/CanvasRenderer";
import { allVars, contextUrl, htmlDecode } from "../shared/default";

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
	
	var downloadOtherFile = function(taskId, dir) {
		var uri = contextUrl + "/tasks/download/" + taskId + "/NEW/" + selectedFileType + "/" + dir + "/";
		window.open(uri);
	};
	
	// file trees
	var selectedFileType;
	var $selectedFile = $();
	
	var filePath = function() {
		return $selectedFile.attr("rel");
	};
	var selectFile = function($file, fileType) {
		selectedFileType = fileType;
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
			
			if (allVars.isUserLoggedIn) {
				
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
			}
			
			/* -------------------------------------------------------
			 * GENERAL TASK - COMMENTS
			 * -------------------------------------------------------
			 */
			
			if (allVars.isUserLoggedIn) {
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
			}
			
			/* -------------------------------------------------------
			 * ASSET TASK
			 * -------------------------------------------------------
			 */
			
			var $assets = $body.find(".task-assets");
			if($assets.length > 0)
			{
				/* -------------------------------------------------------
				 * ASSET TASK - 3D PREVIEW
				 * -------------------------------------------------------
				 */
				
				var $canvasContainer = $assets.find(".task-previews.task-preview-3D");
				if($canvasContainer.find(".task-preview-visual").length > 0) {
					var renderer = PreviewRenderer($canvasContainer);
					$canvasContainer.data("renderer", renderer);// store for unbinding
					
					var $renderOptions = $assets.find(".task-preview-renderOptions");
					// checkboxes
					var $wire = $renderOptions.find(".renderOption-wire input");
					$wire.prop('checked', renderer.getOption("wireframe"));
					$wire.on("change", function() {
						renderer.setOptions({wireframe: $(this).is(":checked")});
					});
					// var $shadows = $renderOptions.find(".renderOption-shadows input");
					// $shadows.prop('checked', renderer.getOption("shadowsEnabled"));
					// $shadows.on("change", function() {
					// 	renderer.setOptions({shadowsEnabled: $(this).is(":checked")});
					// });
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
						var speed = parseFloat(this.value);
						if (!isNaN(speed) && speed < 100) {
							renderer.setOptions({rotateCameraSpeed: speed});
						} else {
							this.value = renderer.getOption("rotateCameraSpeed");
						}
					});
					// render mode
					var modes = [
						{ type: ShadingType.Matcap, selector : ".renderOption-matcap" },
						{ type: ShadingType.Solid, selector : ".renderOption-solid" },
						{ type: ShadingType.None, selector : ".renderOption-none" }
					];
					var setSolidOptionsDisabled = function(shading) {
						var isSolid = shading === ShadingType.Solid;
						$rotLight.prop('disabled', !isSolid);
						$rotLight.closest('.renderOption-rotLight').toggleClass('disabled', !isSolid);
					}
					for (let mode of modes) {
						$renderOptions.find(mode.selector).on("click", function() {
							$renderOptions.find(".renderOption").removeClass("active");
							$(this).addClass("active");
							setSolidOptionsDisabled(mode.type);
							renderer.setOptions({shading: mode.type});
						});
					}
					setSolidOptionsDisabled(ShadingType.Matcap);
				}
				
				if (allVars.isUserLoggedIn) {
					
					/* -------------------------------------------------------
					 * ASSET TASK - ASSET FILES
					 * -------------------------------------------------------
					 */
					var $assetInfo = $assets.find(".task-asset-info");
					var $assetButtons = $assets.find(".task-asset-buttons");
					
					// download
					$assetButtons.filter(".task-asset-original").find(".action-download").click(function() {
						downloadAssetFile(id, 'ORIGINAL');
					});
					$assetButtons.filter(".task-asset-newest").find(".action-download").click(function() {
						downloadAssetFile(id, 'NEW');
					});
					// upload
					// TODO
					//delete
					$assetButtons.filter(".task-asset-newest").find(".action-delete").click(function() {
						var filename = $assetInfo.filter(".task-asset-newest").data("filename");
						var $dialog = Dialogs.confirm(function() {
							Ajax.post(contextUrl + "/tasks/deleteFile/" + id, {asset: true})
								.then(function() {
									Dialogs.hideDialog($dialog);
								});
						}, "Delete newest asset file '" + filename + "' ?");
					});
					
					/* -------------------------------------------------------
					 * ASSET TASK - WIP FILES
					 * -------------------------------------------------------
					 */
					
					// TODO file tree changes need to be events received by all task lists.
					// (instead of just rebuilding the filetree for the current task)
					
					var $files = $body.find(".task-files-wip");
						
					//file trees
					var fileTreeOptions = function(isAsset) {
						return {
							url: contextUrl + "/tasks/files/" + isAsset.toString() + "/" + id,
							directoryClickable: false
						};
					};
					var $fileTreeOther = $files.find(".task-files-wip-tree");
					var createFileTrees = function() {
						$fileTreeOther.fileTree(fileTreeOptions(false),
							function($file) {
								selectFile($file, "WIP");
							}
						);
					};
					createFileTrees();
					
					var $fileOps = $files.find(".task-files-wip-operations");
					
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
					$fileOps.find(".task-file-button.action-upload").click(function() {
						$inputFile.click();
					});
					
					//download
					$fileOps.find(".task-file-button.action-download").click(function() {
						var dir = filePath();
						if (dir === undefined || dir === "") return;
						downloadOtherFile(id, dir);
					});
					
					//delete
					$fileOps.find(".task-file-button.action-delete").click(function() {
						var dir = filePath();
						if (dir === undefined || dir === "") {
							return;
						}
						var $dialog = Dialogs.confirm(function() {
							Ajax.post(contextUrl + "/tasks/deleteFile/" + id,
									{dir: dir, asset: false})
								.then(function() {
									Dialogs.hideDialog($dialog);
									createFileTrees();
								});
						}, "Delete wip file at '" + filePath() + "' ?");
					});
				}
			}
		},

		unbindBody : function($body) {

			/* -------------------------------------------------------
			 * ASSET TASK
			 * -------------------------------------------------------
			 */
			
			var $assets = $body.find(".task-assets");
			if($assets.length > 0)
			{
				/* -------------------------------------------------------
				 * ASSET TASK - 3D PREVIEW
				 * -------------------------------------------------------
				 */

				var $canvasContainer = $assets.find(".task-previews.task-preview-3D");
				if($canvasContainer.find(".task-preview-visual").length > 0) {
					var renderer = $canvasContainer.data("renderer");
					renderer.destroy();
				}
			}
		}
	};
}