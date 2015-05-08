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
	};
	
	return {
		switchDeleteQuestion : function(element) {
			var $delete = $(element).parent().parent().children(".elementDelete");
			$delete.toggle();
		},
		
		findSwitchCommentForm : function(element) {
			switchCommentForm($(element).parents(".task-body").find(".commentForm"));
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
			var $confirm = confirm(
				function(input, textarea) {
					var url = contextUrl + "/tasks/editComment/" + taskId + "/" + commentId;
					Ajax.post(url, {"editedComment" : textarea}) 
						.done(function() {
							hideDialog($confirm);
							alert(reload, "TODO: Refresh task body");
						});
				}, "Change your comment below:", undefined, comment, 700);
		},
		
		uploadFile : function(input, idLink) {
			allVars.$overlay.show();
			var file = input.files[0];
			Ajax.upload(contextUrl + "/tasks/upload/" + idLink, file)
				.done(function(responseText) {
					alert(reload, "TODO: Refresh filetree");
				});
		},
		
		downloadFromPreview : function(idLink, version) {
			var uri = contextUrl + "/tasks/download/" + idLink + "/preview/" + version + "/";
			window.open(uri);
		},
		
		downloadFile : function(idLink) {
			var dir = tasksFuncs.filePath();
			if (dir === undefined || dir === "") {
				return;
			}
			var uri = contextUrl + "/tasks/download/" + idLink + "/" + tasksFuncs.subDir() + "/" + dir + "/";
			window.open(uri);
		},
		
		confirmDeleteFile : function(idLink) {
			var dir = tasksFuncs.filePath();
			if (dir === undefined || dir === "") {
				return;
			}
			var $dialog = confirm(function() {
				Ajax.post(contextUrl + "/tasks/deleteFile/" + idLink,
						{dir: dir, asset: tasksVars.selectedTaskFileIsAsset.toString()})
					.done(function() {
						hideDialog($dialog);
						alert(reload, "TODO: Refresh filetree");
					});
			}, "Delete " + tasksFuncs.filePath() + " ?");
		},
		
		deleteTask : function(idLink, name) {
			var $dialog = confirm(function() {
				Ajax.post(contextUrl + "/tasks/deleteTask/" + idLink)
					.done(function() {
						hideDialog($dialog);
						alert(reload, "TODO: Remove task from client at all places");
					});
			}, "Are you sure you want to delete this task?");
		}
	};
};