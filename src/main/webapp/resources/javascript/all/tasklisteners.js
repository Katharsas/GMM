/**
 * 
 * #### GLOBAL SCOPE FUNCTIONS ###
 * 
 * Listeners for single task element. All the functions of this object are probably going to get
 * mapped to global scope by higher code (until listener data is moved to data attributes and 
 * listeners are bound by javascript code).
 * 
 * Dependencies:
 * 	allVars
 * 	tasksFuncs.selectedTaskFileIsAsset
 * 	tasksFuncs.filePath
 * 	tasksVars.selectedTaskFileIsAsset
 */
var TaskListeners = function(tasksVars, tasksFuncs) {
	
	var reload = function() {
		window.location.reload();
	}
	
	return {
		switchDeleteQuestion : function(element) {
			var $delete = $(element).parent().parent().children(".elementDelete");
			$delete.toggle();
		},
		
		findSwitchCommentForm : function(element) {
			switchCommentForm($(element).parents(".listElementBody").find(
					".commentForm"));
		},
		
		switchCommentForm : function($commentForm) {
			if ($commentForm.is(":visible")) {
				hideCommentForm($commentForm);
			} else {
				showCommentForm($commentForm);
			}
		},
		
		hideCommentForm : function($commentForm) {
			var $elementComments = $commentForm.parent();
			if ($elementComments.is(":visible:blank")) {
				$elementComments.hide();
			}
			$commentForm.hide();
		},
		
		showCommentForm : function ($commentForm) {
			$commentForm.parent().show();
			$commentForm.show();
		},
		
		changeComment : function(comment, taskId, commentId) {
			confirm(function() {confirmCommentChange(taskId, commentId);},
					"Bitte Kommentar ändern",
					undefined,
					comment);
		},
		
		confirmCommentChange : function(taskId, commentId) {
			var comment = $("#confirmDialogTextArea").val();
			var url = allVars.contextPath + "/tasks/editComment/" + taskId + "/" + commentId;
			$.post(url, {"editedComment" : comment}, 
				function() {
					alert(reload, "TODO: Refresh task body");
				}
			);
		},
		
		uploadFile : function(input, idLink) {
			allVars.$overlay.show();
			
			var file = input.files[0];
			var uri = allVars.contextPath + "/tasks/upload/" + idLink;
		
			sendFile(file, uri, function(responseText) {
				alert(reload, "TODO: Refresh filetree");
			});
		},
		
		downloadFromPreview : function(idLink, version) {
			var uri = allVars.contextPath + "/tasks/download/" + idLink + "/preview/" + version + "/";
			window.open(uri);
		},
		
		downloadFile : function(idLink) {
			var dir = tasksFuncs.filePath();
			if (dir === undefined || dir === "") {
				return;
			}
			var uri = allVars.contextPath + "/tasks/download/" + idLink + "/" + tasksFuncs.subDir() + "/" + dir + "/";
			window.open(uri);
		},
		
		confirmDeleteFile : function(idLink) {
			var dir = tasksFuncs.filePath();
			if (dir === undefined || dir === "") {
				return;
			}
			confirm(function() {
				$.post(allVars.contextPath + "/tasks/deleteFile/" + idLink, {dir: dir,
						asset: tasksVars.selectedTaskFileIsAsset.toString()},
					function() {
						alert(reload, "TODO: Refresh filetree");
				});
			}, "Delete " + tasksFuncs.filePath() + " ?");
		},
		
		deleteTask : function(idLink, name) {
			confirm(function() {
					$.post(allVars.contextPath + "/tasks/deleteTask/" + idLink, {},
						function() {
							alert(reload, "TODO: Remove task from client");
					});
				}, "Are you sure you want to delete this task?");
		}
	};
};