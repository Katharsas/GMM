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

var ajaxChannel;

function loadTasks() {
	ajaxChannel = new ResponseBundleHandler('tasks');
	ajaxChannel.start();
}

function importAssets(assetTypes) {
	ajaxChannel = new ResponseBundleHandler("assets");
	ajaxChannel.start(assetTypes);
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
function ResponseBundleHandler(responseBundleOption) {
//	var Option = {startURI:undefined, nextURI:undefined, conflicts:[], showButtons:undefined};
	//TODO make Option interface for options (for auto-complete)
	var ResponseBundleOptions = {
			tasks : {
				nextURI : "admin/load/next",
				conflicts : ["conflict"],
				showButtons : function(conflict, $options) {
					$options.children("#skipButton").show();
					$options.children("#doForAllCheckbox").show();
					$options.children("#overwriteTaskButton").show();
					$options.children("#addBothTasksButton").show();
				},
				start : function(reactToResults, showException) {
					var dir = allVars.selectedBackupFile.attr('rel');
					if(dir === undefined || dir === "") {
						return undefined;
					}
					return $.getJSON("admin/load", { dir: dir })
						.done(reactToResults).fail(showException);
				}

			},
			assets : {
				nextURI : "admin/importAssets/next",
				conflicts : ["taskConflict", "folderConflict"],
				showButtons : function(conflict, $options) {
					$options.children("#skipButton").show();
					$options.children("#doForAllCheckbox").show();
					switch(conflict) {
					case "taskConflict":
						$options.children("#overwriteTaskAquireDataButton").show();
						$options.children("#overwriteTaskDeleteDataButton").show();
						break;
					case "folderConflict":
						$options.children("#aquireDataButton").show();
						$options.children("#deleteDataButton").show();
						break;
					}
				},
				start : function(reactToResults, showException, assetTypes) {
					var textures;
					console.log(assetTypes);
					if (assetTypes === "textures") textures = true;
					else if (assetTypes === "models") textures = false;
					else return undefined;
					$("#taskForm").ajaxSubmit({data: { textures: textures },
						success: reactToResults, error: showException});
					//TODO return ajaxResult whitch functions which get called by ajaxSubmit
					// or use a submit which returns getJSON like result.
					return true;
				}
			}
		};
	var options = ResponseBundleOptions[responseBundleOption];
//	var options = Option;
	
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
				else  {
					//TODO: contains method for arrays?
					//START contains
					var contains = false;
					for (var i = 0; i < options.conflicts.length; i++) {
						if (data.status === options.conflicts[i]) {
							contains = true;
						}
					}
					//END contains
					$conflictMessage.html(data.message);
					if (contains) options.showButtons(data.status, $conflictOptions);
					else {
						$conflictMessage.html("Something went wrong! " +
								"Out of sync with server. Please reload page.");
					}
				}
			}
		});
	}
	
	/**
	 * Get first responses (bundled) from server. Last response in bundle is either either "finish"
	 * or conflict message.
	 */
	this.start = function (startOptions) {
		$conflictOptions.children().hide();
		$finishedButton.hide();
		ajaxResult = options.start(reactToResults, showException, startOptions);
		console.log(ajaxResult);
		if (ajaxResult === undefined) return;
		//TODO if ajaxResult has function done and fail, use them
//		ajaxResult.done(reactToResults).fail(showException);
		showDialogue("#loadTasksDialog");
	};
	
	/**
	 * If a conflict occured at last bundle, use this method to give an answer for conflict handling.
	 * Server will return another response bundle.
	 */
	this.answer = function (answer) {
		$conflictOptions.children().hide();
		$conflictMessage.empty();
		var doForAll = $checkBox.is(":checked");
		$.getJSON(options.nextURI, { operation: answer, doForAll: doForAll })
			.done(reactToResults)
			.fail(showException);
	};
	
	this.finish = function () {
		hideDialogue();
		$messageList.empty();
	};
}