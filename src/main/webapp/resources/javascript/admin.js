$(document).ready( function() {
	hideImport();
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

function loadTasks() {
	var dir = allVars.selectedBackupFile.attr('rel');
	if(dir === undefined || dir === "") {
		return;
	}
	$("#conflictOptions").hide();
	$("#finishLoadingButton").hide();
	showDialogue("#loadTasksDialog");
	$.getJSON("admin/load", { dir: dir }, handleLoadAnswer);
}

function loadTasksNext(answer) {
	$("#conflictOptions").hide();
	$("#conflictMessage").empty();
	var doForAll = $('#doForAllCheckbox input').is(":checked");
	loadTasksNextElement(answer, doForAll);
}

function loadTasksNextElement(answer, doForAll) {
	$.getJSON("admin/load/next", { operation: answer, doForAll: doForAll }, handleLoadAnswer);
}

function handleLoadAnswer(data) {
	if(data.status == "finished") {
		$("#finishLoadingButton").show();
	}
	else {
		if(data.status == "conflict") {
			$("#conflictMessage").html(data.message);
			$("#conflictOptions").show();
		}
		else {
			$("#loadedTasks ul").append("<li>"+data.message+"</li>");
			loadTasksNextElement("default", false);
		}
	}
	return;
}
function finishTaskLoading() {
	hideDialogue();
	$("#loadedTasks ul").empty();
}

function deleteFile() {
	var dir = allVars.selectedBackupFile.attr('rel');
	if(dir === undefined || dir === "") {
		return;
	}
	$.post("admin/deleteFile", { dir: dir }, refreshTaskBackups);
}

function deleteAllTasks() {
	$.post("admin/deleteTasks");
}

function saveAllTasks() {
	$("#saveAllTasksForm").submit();
}

function importAssets(textures) {
	$("#overlay").show();
	$.post("admin/importAssets", { textures: textures }, function() {
		$("#overlay").hide();
	});
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
	$.post("admin/import/cancel", function() {
		hideImport();
	});
}

function editUserRole(idLink, userRole) {
	$textField = $("#confirmDialog").find("#confirmDialogTextInput");
	confirm(function () {
		var role = $textField.attr("value");
		$.post("admin/users/edit/"+idLink,
				{"role": role}, function() {
					window.location.reload();
				});
	}, "Valid roles: ROLE_USER, ROLE_ADMIN", userRole);
}

function switchAdmin(idLink) {
	$.post("admin/users/admin/"+idLink, function() {
		var $role = $("#"+idLink+" .subElementUserRole");
		if($.trim($role.html())==="[ADMIN]") {
			$role.html("&nbsp;");
		}
		else {
			$role.html("[ADMIN]");
		}
	});
}

function editUserName(idLink, userName) {
	$textField = $("#confirmDialog").find("#confirmDialogTextInput");
	confirm(function () {
		var name = $textField.attr("value");
		$.post("admin/users/edit/"+idLink,
				{"name": name}, function() {
					window.location.reload();
				});
	}, "Enter user name here:", userName);
}

function resetPassword(idLink) {
	confirm(function() {
		$.post("admin/users/reset/"+idLink, function(data) {
			hideDialogue();
			confirm(function() {
				hideDialogue();
				window.location.reload();
			}, "New Password:", data);
		});
	}, "Generate New Random Password?");
}

function switchUser(idLink, userName) {
	$.post("admin/users/switch/"+idLink, function() {
		var $enabled = $("#"+idLink+" .subElementUserEnabled");
		console.log($.trim($enabled.html()));
		if($.trim($enabled.html()).charCodeAt(0)===0x2611) {
			$enabled.html("&#x2610;");
		}
		else {
			$enabled.html("&#x2611;");
		}
	});
}

function saveUsers() {
	$.post("admin/users/save");
}

function loadUsers() {
	confirm(function() {
		$.post("admin/users/load", function() {
			window.location.reload();
		});
	}, "Delete all unsaved user data?");
}