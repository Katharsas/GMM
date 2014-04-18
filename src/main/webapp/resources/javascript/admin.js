var adminVars = {
	"selectedAssetFile":$(),
	"selectedBackupFile":$()
};
var adminFuncs = {
	"selectTreeElement":
		function($newFile, marker) {
			var $oldFile = adminVars[marker];
			$($oldFile).removeClass(marker);
			$($newFile).addClass(marker);
			adminVars[marker] = $newFile;
		},
	"treePluginOptions":
		function(mapping, directories) {
			return {
				root : "",
				script : mapping,
				expandSpeed : 300,
				collapseSpeed : 300,
				directoryClickable : directories
			};
		}
};

$(document).ready( function() {
	cancelImport();
	refreshTaskBackups();
	refreshTaskImportTree();
});

function refreshTaskBackups() {
	$('#taskBackupsContainer').fileTree(
		adminFuncs.treePluginOptions("admin/backups", false),
		function($file) {
			adminFuncs.selectTreeElement($file, "selectedBackupFile");
		}
	);
}

function loadTasks() {
	var dir = adminVars["selectedBackupFile"].attr('rel');
	if(dir == "") {
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

function deleteAllTasks() {
	window.location = "admin/deleteTasks";
}

function saveAllTasks() {
	$("#saveAllTasksForm").submit();
}

function importAssets() {
	alert("Not yet implemented");
}

function refreshTaskImportTree() {
	$('#fileTreeContainer').fileTree(
		adminFuncs.treePluginOptions("admin/originalAssets", true),
		function($file) {
			adminFuncs.selectTreeElement($file, "selectedAssetFile");
		}
	);
}

function addAssetPaths(textures) {
	var dir = adminVars["selectedAssetFile"].attr('rel');
	$("#selectedPaths ul").empty();
	$.getJSON("admin/getAssetPaths", { dir: dir, textures: textures }, function(paths) {
		if(paths.length==0) {
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

function cancelImport() {
	$("#selectedPaths ul").empty();
	$('#importButtons .button').hide();
	$('#taskForm').hide();
	$('#addMeshesButton').show();
	$('#addTexturesButton').show();
}

function importTextures() {
	alert("Not yet implemented!");
}
function importMeshes() {
	alert("Not yet implemented!");
}