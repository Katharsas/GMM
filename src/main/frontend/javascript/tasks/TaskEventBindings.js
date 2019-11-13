import $ from "../lib/jquery";
import Ajax from "../shared/ajax";
import Dialogs from "../shared/dialogs";
import { switchPinOperation } from "./Task";
import TaskDialogs from "../shared//TaskDialog";
import EventListener from "../shared/EventListener";
import PreviewRenderer from "../shared/preview/PreviewRenderer";
import { ShadingType } from "../shared/preview/CanvasRenderer";
import AssetFileOperationsNotifier from "../shared/AssetFileOperationsNotifier";
import { allVars, contextUrl, htmlDecode } from "../shared/default";

/**
 * ------------- TaskEventBindings ---------------------------------------------
 * Provides methods to bind all needed listeners to task header or body
 * 
 */

let $folderDialogTemplate;
let $folderDialogContainer;

$(document).ready(function() {
	$folderDialogTemplate = $("#folderDialog-template");
    $folderDialogContainer = $("#folderDialog-container");
});

export default function(onedit, setIsPinned, isPinned) {
	
	const updateTaskLists = function() {
		EventListener.trigger(EventListener.events.TaskDataChangeEvent);
	};
	const updatePinnedList = function() {
		EventListener.trigger(EventListener.events.PinnedListChangeEvent);
	};
	
	//comments
	const isCommentFormShown = function($commentForm) {
		return $commentForm.is(":visible");
	}

	const hideCommentForm = function($commentForm) {
		const $elementComments = $commentForm.parent();
		if ($elementComments.is(":visible:blank")) {
			$elementComments.hide();
		}
		$commentForm.hide();
	};
	
	const showCommentForm = function ($commentForm) {
		$commentForm.parent().show();
		$commentForm.show();
	};
	
	//download
	const downloadAssetFile = function(taskId, groupType) {
		const uri = contextUrl + "/tasks/download/" + taskId + "/" + groupType + "/ASSET/";
		window.open(uri);
	};
	
	const downloadOtherFile = function(taskId, dir) {
		const uri = contextUrl + "/tasks/download/" + taskId + "/NEW/" + selectedFileType + "/" + dir + "/";
		window.open(uri);
	};
	
	// file trees
	let selectedFileType;
	let $selectedFile = $();
	
	const filePath = function($fileOrFolder) {
		return $fileOrFolder.attr("rel");
	};
	const selectFile = function($file, fileType) {
		selectedFileType = fileType;
		$selectedFile.removeClass("task-files-selected");
		$selectedFile = $file;
		$selectedFile.addClass("task-files-selected");
	};
	
	
	return {

		bindHeader : function($header) {},
		
		/**
		 * @param {JQuery} $body - Complete body part of a task, bind functions to this (or its children).
		 * @param {Function} returnFocus - Call to return focus from nested elements back to the task itself (optional).
		 */
		bindBody : function(id, $body, returnFocus) {
			if (returnFocus === undefined) returnFocus = function() {};

			/* -------------------------------------------------------
			 * GENERAL TASK
			 * -------------------------------------------------------
			 */
			
			const $operations = $body.find(".task-body-footer").children(".task-operations");
			
			if (allVars.isUserLoggedIn) {
				
				//edit task
				$operations.find(".task-operations-editTask").click(function() {
					onedit(id);
				});
				//delete task
				$operations.find(".task-operations-deleteTask").click(function() {
					const $confirm = Dialogs.confirm(function() {
						Ajax.post(contextUrl + "/tasks/deleteTask/" + id)
						.then(function() {
							Dialogs.hideDialog($confirm);
							updateTaskLists();
						});
					}, "Are you sure you want to delete this task?");
				});

				const isTaskPinned = isPinned(id);
				switchPinOperation($operations, isTaskPinned);
				//pin task
				$operations.find(".task-operations-pin").click(function() {
					Ajax.post(contextUrl + "/tasks/pinned/pin", { idLink: id })
					.then(function() {
						setIsPinned(id, true);
						updatePinnedList();
					});
				});
				// unpin task
				$operations.find(".task-operations-unpin").click(function() {
					Ajax.post(contextUrl + "/tasks/pinned/unpin", { idLink: id })
					.then(function() {
						setIsPinned(id, false);
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
				const $comments = $body.find(".task-comments");
				const $form = $comments.children("form.task-comments-form");
				const $formTextarea = $form.find("textarea.task-comments-form-textArea");
				const $commentsSwitch = $operations.find(".task-operations-switchComment");
				$form.on("keyup", function (event) {
					if (event.key === "Escape") {
						if (isCommentFormShown($form)) {
							event.stopPropagation();
							$commentsSwitch.click();
						}
					}
				});
				//show comment edit dialog
				$comments.on("click", ".task-comment-editButton", function() {
					const $comment = $(this).parent(".task-comment");
					const comment = htmlDecode($comment.children(".task-comment-text").html());
					const commentId = $comment.attr("id");
					const $confirm = Dialogs.confirm(
						function(input, textarea) {
							const url = contextUrl + "/tasks/editComment/" + id + "/" + commentId;
							Ajax.post(url, {"editedComment" : textarea}) 
								.then(function() {
									Dialogs.hideDialog($confirm);
									updateTaskLists();
								});
						}, "Change your comment below:", undefined, comment, 500, 150);
				});
				//show/hide new comment form
				$commentsSwitch.click(function() {
					if (isCommentFormShown($form)) {
						hideCommentForm($form);
						returnFocus();
					}
					else {
						showCommentForm($form);
						$formTextarea.focus();
					}
				});
				//submit new comment
				$form.find(".task-comment-form-submitButton").click(function() {
					const url = contextUrl + "/tasks/submitComment/" + id;
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
			
			const $assets = $body.find(".task-assets");
			if($assets.length > 0)
			{
				/* -------------------------------------------------------
				 * ASSET TASK - 3D PREVIEW
				 * -------------------------------------------------------
				 */
				
				const $canvasContainer = $assets.find(".task-previews.task-preview-3D");
				const $optionsContainer = $assets.find(".task-preview-options");
				if($canvasContainer.find(".task-preview-visual").length > 0) {
					const renderer = PreviewRenderer($canvasContainer);
					$canvasContainer.data("renderer", renderer);// store for unbinding
					
					const $renderOptions = $optionsContainer.find(".task-preview-renderOptions");
					// checkboxes
					const $wire = $renderOptions.find(".renderOption-wire input");
					$wire.prop('checked', renderer.getOption("wireframe"));
					$wire.on("change", function() {
						renderer.setOptions({wireframe: $(this).is(":checked")});
					});
					// const $shadows = $renderOptions.find(".renderOption-shadows input");
					// $shadows.prop('checked', renderer.getOption("shadowsEnabled"));
					// $shadows.on("change", function() {
					// 	renderer.setOptions({shadowsEnabled: $(this).is(":checked")});
					// });
					const $rotLight = $renderOptions.find(".renderOption-rotLight input");
					$rotLight.prop('checked', renderer.getOption("rotateLight"));
					$rotLight.on("change", function() {
						renderer.setOptions({rotateLight: $(this).is(":checked")});
					});
					const $rotCamera = $renderOptions.find(".renderOption-rotCamera input");
					$rotCamera.prop('checked', renderer.getOption("rotateCamera"));
					$rotCamera.on("change", function() {
						renderer.setOptions({rotateCamera: $(this).is(":checked")});
					});
					// number input
					const $rotCameraSpeed = $renderOptions.find(".renderOption-rotCameraSpeed");
					$rotCameraSpeed.val(renderer.getOption("rotateCameraSpeed"));
					$rotCameraSpeed.on("input", function() {
						const speed = parseFloat(this.value);
						if (!isNaN(speed) && speed < 100) {
							renderer.setOptions({rotateCameraSpeed: speed});
						} else {
							this.value = renderer.getOption("rotateCameraSpeed");
						}
					});
					// render mode
					const modes = [
						{ type: ShadingType.Matcap, selector : ".renderOption-matcap" },
						{ type: ShadingType.Solid, selector : ".renderOption-solid" },
						{ type: ShadingType.None, selector : ".renderOption-none" }
					];
					const setSolidOptionsDisabled = function(shading) {
						const isSolid = shading === ShadingType.Solid;
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
					// maximized view
					const $maximize = $canvasContainer.find(".task-preview-maximize .button");
					let isMaximized = false;
					$canvasContainer.on("keyup", function (event) {
						if (event.key === "Escape") {
							if (isMaximized) {
								event.stopPropagation();
								$maximize.click();
							}
						}
					});
					let draggableTransform;
					$maximize.on("click", function() {
						isMaximized = !isMaximized;
						$assets.find("table").toggleClass("maximized", isMaximized);
						$canvasContainer.find(".task-preview-visual").toggleClass("maximized", isMaximized);
						//$canvasContainer.find("canvas").removeAttr("width");
						$canvasContainer.find("canvas").removeAttr("height");
						// task dialog
						const $draggable = $body.closest(".draggable");
						if (isMaximized) {
							draggableTransform = $draggable.css("transform");
							$draggable.css("transform", "");
							$canvasContainer.focus();
						} else {
							$draggable.css("transform", draggableTransform);
							draggableTransform = undefined;
							returnFocus();
						}
					});

					/* -------------------------------------------------------
					 * ASSET TASK - DEPENDENCIES
					 * -------------------------------------------------------
					 */

					// linked texture tasks in model tasks
					const $textures = $assets.find(".task-asset-model-textures-tasks li");
					for (let listEntry of $textures) {
						let $listEntry = $(listEntry);
						const idLink = $listEntry.data("id");
						$listEntry.on("click", function() {
							TaskDialogs.openDialog(idLink);
						});
					}
				}
				
				if ($assets.length > 0 && allVars.isUserLoggedIn) {
					
					/* -------------------------------------------------------
					 * ASSET TASK - ASSET FILES
					 * -------------------------------------------------------
					 */
					const $assetInfo = $assets.find(".task-asset-info");
					const $assetButtonsOriginal = $assets.find(".task-asset-buttons").filter(".task-asset-original");
					const $assetButtonsNewest = $assets.find(".task-asset-buttons").filter(".task-asset-newest");
					
					// download
					const $downloadOriginalAsset = $assetButtonsOriginal.find(".action-download");
					$downloadOriginalAsset.click(function () {
						downloadAssetFile(id, 'ORIGINAL');
					})
					const $downloadNewestAsset = $assetButtonsNewest.find(".action-download");
					AssetFileOperationsNotifier.registerNewAssetOperation($downloadNewestAsset, "click", function() {
						downloadAssetFile(id, 'NEW');
					});
					// upload
					const $uploadNewestAssetInput = $assetButtonsNewest.find(".action-upload-input");
					AssetFileOperationsNotifier.registerNewAssetOperation($uploadNewestAssetInput, "change", function() {
						Dialogs.showOverlay();
						const file = $uploadNewestAssetInput[0].files[0];
						Ajax.upload(contextUrl + "/tasks/upload/ASSET/" + id, file)
							.then(function() {
								Dialogs.hideOverlay();
							});
					});
					const $uploadNewestAsset = $assetButtonsNewest.find(".action-upload");
					$uploadNewestAsset.click(function() {
						$uploadNewestAssetInput.click();
					});
					// create folder
					const $createFolder = $assetButtonsNewest.find(".action-folder");
					let $selectedFolder = null;
					$createFolder.on("click", function() {

						const { $dialog, actionCancel } = Dialogs.createDialog($folderDialogTemplate, $folderDialogContainer);
						
						const $input = $dialog.find(".folderDialog-path-input");
						$input.on("input", function() {
							if ($selectedFolder !== null) {
								const matchesTree = filePath($selectedFolder) === $input.val();
								$selectedFolder.toggleClass("selected", matchesTree);
							}
						});

						const $fileTree = $dialog.find(".folderDialog-tree");
						const onSelectFolder = function($folder) {
							$input.val(filePath($folder));
							if ($selectedFolder !== null) {
								$selectedFolder.removeClass("selected");
							}
							$selectedFolder = $folder;
							$selectedFolder.addClass("selected");
						};
						$fileTree.fileTree({
							url : contextUrl + "/tasks/newAssetFolder/" + id,
							fileClickable : false,
							multiFolder : false
						}, onSelectFolder);

						Dialogs.showDialog($dialog);

						const $confirmButton = $dialog.find(".folderDialog-ok");
						AssetFileOperationsNotifier.registerNewAssetOperation($confirmButton, "click", function() {
							const path = $input.val();
							Ajax.post(contextUrl + "/tasks/createAssetFolder/" + id, {path})
								.then(actionCancel);
						});
						const $cancelButton = $dialog.find(".folderDialog-cancel");
						$cancelButton.on("click", actionCancel);
					});
					//delete
					const $deleteNewAssetFile = $assetButtonsNewest.find(".action-delete");
					AssetFileOperationsNotifier.registerNewAssetOperation($deleteNewAssetFile, "click", function() {
						const filename = $assetInfo.filter(".task-asset-newest").data("filename");
						const $dialog = Dialogs.confirm(function() {
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
					const $files = $body.find(".task-files-wip");
						
					//file trees
					const fileTreeOptions = function(isAsset) {
						return {
							url: contextUrl + "/tasks/files/" + isAsset.toString() + "/" + id,
							directoryClickable: false
						};
					};
					const $fileTreeOther = $files.find(".task-files-wip-tree");
					$fileTreeOther.fileTree(fileTreeOptions(false), function($file) {
						selectFile($file, "WIP");
						// TODO put path into filetree container data instead of globally 
					});
					
					const $fileOps = $files.find(".task-files-wip-operations");
					
					//upload
					const $uploadWipInput = $fileOps.find(".action-upload-input");
					AssetFileOperationsNotifier.registerNewAssetOperation($uploadWipInput, "change", function() {
						Dialogs.showOverlay();
						const file = $uploadWipInput[0].files[0];
						Ajax.upload(contextUrl + "/tasks/upload/WIP/" + id, file)
							.then(function() {
								Dialogs.hideOverlay();
							});
					});
					const $uploadWip = $fileOps.find(".task-file-button.action-upload");
					$uploadWip.click(function() {
						$uploadWipInput.click();
					});
					
					//download
					const $downloadWipFile = $fileOps.find(".task-file-button.action-download");
					AssetFileOperationsNotifier.registerNewAssetOperation($downloadWipFile, "click", function() {
						const dir = filePath($selectedFile);
						if (dir === undefined || dir === "") return;
						downloadOtherFile(id, dir);
					});
					
					//delete
					const $deleteWipFile = $fileOps.find(".task-file-button.action-delete");
					AssetFileOperationsNotifier.registerNewAssetOperation($deleteWipFile, "click", function() {
						const dir = filePath($selectedFile);
						if (dir === undefined || dir === "") {
							return;
						}
						const $dialog = Dialogs.confirm(function() {
							Ajax.post(contextUrl + "/tasks/deleteFile/" + id,
									{dir: dir, asset: false})
								.then(function() {
									Dialogs.hideDialog($dialog);
								});
						}, "Delete wip file at '" + filePath($selectedFile) + "' ?");
					});
				}
			}
		},

		unbindBody : function($body) {

			/* -------------------------------------------------------
			 * ASSET TASK
			 * -------------------------------------------------------
			 */
			
			const $assets = $body.find(".task-assets");
			if($assets.length > 0)
			{
				/* -------------------------------------------------------
				 * ASSET TASK - 3D PREVIEW
				 * -------------------------------------------------------
				 */

				const $canvasContainer = $assets.find(".task-previews.task-preview-3D");
				if($canvasContainer.find(".task-preview-visual").length > 0) {
					const renderer = $canvasContainer.data("renderer");
					renderer.destroy();
				}
			}
		}
	};
}