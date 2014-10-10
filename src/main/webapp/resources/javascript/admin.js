$(document).ready( function() {
	hideImport();
	hideTaskFormType();
	refreshTaskBackups();
	refreshTaskImportTree();
});

function refreshTaskBackups() {
	$('#taskBackupsContainer').fileTree(
		allFuncs.treePluginOptions("admin/backups", false),
		function($file) {
			allFuncs.selectTreeElement($file, "selectedBackupFile");
		}
	);
}

function hideTaskFormType() {
	$("#taskForm").find("#taskGroupType").hide();
}

function deleteFile() {
	var dir = allVars.selectedBackupFile.attr('rel');
	if(dir === undefined || dir === "") {
		return;
	}
	$.post("admin/deleteFile", { dir: dir })
		.done(refreshTaskBackups)
		.fail(showException);
}

function deleteAllTasks() {
	$.post("admin/deleteTasks").fail(showException);
}

function saveAllTasks() {
	$("#saveAllTasksForm").submit();
}

function importAssets(textures) {
	$("#overlay").show();
	$("#taskForm").ajaxSubmit({
			data: { textures: textures },
			success: function() {$("#overlay").hide();},
			error: showException});
}

function refreshTaskImportTree() {
	$('#originalAssetsContainer').fileTree(
		allFuncs.treePluginOptions("admin/originalAssets", true),
		function($file) {
			allFuncs.selectTreeElement($file, "selectedAssetFile");
		}
	);
}

function addAssetPaths(textures) {
	var dir = allVars.selectedAssetFile.attr('rel');
	$("#selectedPaths ul").empty();
	$.getJSON("admin/getAssetPaths", { dir: dir, textures: textures }, function(paths) {
		if(paths.length===0) {
			cancelImport();
			return;
		}
		for(var i in paths) {
			$("#selectedPaths ul").append("<li>"+paths[i]+"</li>");
		}
	});
	
	if(textures) {
		$('#importTexturesButton').show();
		$('#importMeshesButton').hide();
		$('#addMeshesButton').hide();
	}
	else {
		$('#importMeshesButton').show();
		$('#importTexturesButton').hide();
		$('#addTexturesButton').hide();
	}
	$('#cancelImportButton').show();
	$('#taskForm').show();
}

function hideImport() {
	$("#selectedPaths ul").empty();
	$('#importButtons .button').hide();
	$('#taskForm').hide();
	$('#addMeshesButton').show();
	$('#addTexturesButton').show();
}

function cancelImport() {
	$.post("admin/import/cancel")
		.done(function() {hideImport();})
		.fail(showException);
}

function editUserRole(idLink, userRole) {
	var $textField = $("#confirmDialog").find("#confirmDialogTextInput");
	confirm(function () {
		var role = $textField.attr("value");
		$.post("admin/users/edit/"+idLink, {"role": role})
			.done(function() {
				window.location.reload();
			})
			.fail(showException);
	}, "Valid roles: ROLE_USER, ROLE_ADMIN", userRole);
}

function switchAdmin(idLink) {
	$.post("admin/users/admin/"+idLink)
		.done(function() {
			var $role = $("#"+idLink+" .subElementUserRole");
			if($.trim($role.html())==="[ADMIN]") {
				$role.html("&nbsp;");
			}
			else {
				$role.html("[ADMIN]");
			}
		})
		.fail(showException);
}

function editUserName(idLink, userName) {
	var $textField = $("#confirmDialog").find("#confirmDialogTextInput");
	confirm(function () {
		var name = $textField.attr("value");
		$.post("admin/users/edit/"+idLink, {"name": name})
			.done(function() {
				window.location.reload();
			})
			.fail(showException);
	}, "Enter user name here:", userName);
}

function resetPassword(idLink) {
	confirm(function() {
		$.post("admin/users/reset/"+idLink)
			.done(function(data) {
				hideDialogue();
				alert(function() {
					hideDialogue();
					window.location.reload();},
					"New Password:",data);
				})
			.fail(showException);
	}, "Generate New Random Password?");
}

function switchUser(idLink, userName) {
	$.post("admin/users/switch/"+idLink)
		.done(function() {
			var $enabled = $("#"+idLink+" .subElementUserEnabled");
			console.log($.trim($enabled.html()));
			if($.trim($enabled.html()).charCodeAt(0)===0x2611) {
				$enabled.html("&#x2610;");
			}
			else {
				$enabled.html("&#x2611;");
			}
		})
		.fail(showException);
}

function saveUsers() {
	$.post("admin/users/save").fail(showException);
}

function loadUsers() {
	confirm(function() {
		$.post("admin/users/load")
			.done(function() {
				window.location.reload();
			})
			.fail(showException);
	}, "Delete all unsaved user data?");
}


/**
 * -------------------- ResponseBundleHandler -----------------------------------------------------
 * Provides a way to communicate with the server when the server needs to send a lot of
 * messages to the client and the client needs to be able to respond to any of those messages.
 * 
 * To not waste requests, the messages from the server will come in bundles of dynamic size, where
 * the last bundle message either indicates that the server needs a message from the client or that
 * the server has finished sending all messages.
 */
function ResponseBundleHandler() {
	var $messageList = $("#loadedTasks ul");
	var $conflictMessage  = $("#conflictMessage");
	var $conflictOptions = $("#conflictOptions");
	var $finishedButton = $("#finishLoadingButton");
	var $checkBox = $('#doForAllCheckbox input');
	
	/**
	 * process server response bundle
	 */
	function reactToResults(results) {
		results.forEach(function(data) {
			//all elements before last should be success
			if(data.status == "success") {
				$messageList.append("<li>"+data.message+"</li>");
			}
			//last element is conflict or finished
			else {
				if(data.status == "finished") {
					$finishedButton.show();
				}
				else if (data.status == "conflict") {
					$conflictMessage.html(data.message);
					$conflictOptions.show();
				}
				else {
					$conflictMessage.html("Something went wrong! " +
							"Out of sync with server. Please reload page.");
				}
			}
		});
	}
	
	/**
	 * Get first responses (bundled) from server. Last response in bundle is either either "finish"
	 * or conflict message.
	 */
	this.start = function () {
		var dir = allVars.selectedBackupFile.attr('rel');
		if(dir === undefined || dir === "") {
			return;
		}
		$conflictOptions.hide();
		$finishedButton.hide();
		showDialogue("#loadTasksDialog");
		$.getJSON("admin/load", { dir: dir })
			.done(reactToResults)
			.fail(showException);
	};
	
	/**
	 * If a conflict occured at last bundle, use this method to give an answer for conflict handling.
	 * Server will return another response bundle.
	 */
	this.answer = function (answer) {
		$conflictOptions.hide();
		$conflictMessage.empty();
		var doForAll = $checkBox.is(":checked");
		console.log(answer);
		console.log(doForAll);
		$.getJSON("admin/load/next", { operation: answer, doForAll: doForAll })
			.done(reactToResults)
			.fail(showException);
	};
	
	this.finish = function () {
		hideDialogue();
		$messageList.empty();
	};
}