//TODO convert to event listener attaching below
var GlobalTaskListeners = function(tasksVars, tasksFuncs) {
	var reload = function() {
		window.location.reload();
	};
	
	return {
		
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
		
		
	};
};


/**
 * ------------- TaskEventBindings ---------------------------------------------
 * Provides methods to bind all needed listeners to task header or body
 * 
 * Dependencies:
 * 	allVars
 * 	tasksFuncs.selectedTaskFileIsAsset
 * 	tasksFuncs.filePath
 * 	tasksVars.selectedTaskFileIsAsset
 */
var TaskEventBindings = function(tasksVars, tasksFuncs, onswitch, onchange, onremove) {
	
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
	
	var changeComment = function(comment, taskId, commentId, $task) {
		var $confirm = confirm(
			function(input, textarea) {
				var url = contextUrl + "/tasks/editComment/" + taskId + "/" + commentId;
				Ajax.post(url, {"editedComment" : textarea}) 
					.done(function() {
						hideDialog($confirm);
						onchange($task);
					});
			}, "Change your comment below:", undefined, comment, 700);
	};
	
	var deleteTask = function(taskId, $task) {
		var $confirm = confirm(function() {
			Ajax.post(contextUrl + "/tasks/deleteTask/" + taskId)
				.done(function() {
					hideDialog($confirm);
					onremove($task);
				});
		}, "Are you sure you want to delete this task?");
	};
	
	var reload = function() {
		window.location.reload();
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
			
			//comments
			var $comments = $body.find(".task-comments");
			var $form = $comments.children("form.task-comments-form");
			var $operations = $body.find(".task-body-footer").children(".task-operations");
			//show comment edit dialog
			$comments.on("click", ".task-comment-editButton", function(event) {
				var $comment = $(this).parent(".task-comment");
				var $text = $comment.children(".task-comment-text");
				changeComment(htmlDecode($text.html()), id, $comment.attr("id"), $task);
			});
			//show/hide new comment form
			$operations.find(".task-operations-switchComment").click(function(event) {
				if ($form.is(":visible")) hideCommentForm($form);
				else showCommentForm($form);
			});
			//submit new comment
			$form.find(".task-comment-form-submitButton").click(function(event) {
				var url = contextUrl + "/tasks/submitComment/" + id;
				Ajax.post(url, {}, $form)
					.done(function() {
						onchange($task);
					});
			});
			
			//delete task
			$operations.find(".task-operations-deleteTask").click(function(event) {
				deleteTask(id, $task);
			});
		}
	};
};