$(document).ready( function() {
	hideImport();
	hideTaskFormType();
	refreshTaskBackups();
	refreshTaskImportTree();
	
	var $adminBanner = $("#adminBannerTextArea");
	$adminBanner.html(allVars.adminBanner);
	$adminBanner.blur(function() {
		$.post(allVars.contextPath+"/admin/changeBannerMessage", {message: $adminBanner.val()})
			.fail(showException);
	});
});

function refreshTaskBackups() {
	$('#taskBackupsContainer').fileTree(
		allFuncs.treePluginOptions(allVars.contextPath+"/admin/backups", false),
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
	$.post(allVars.contextPath+"/admin/deleteFile", { dir: dir })
		.done(refreshTaskBackups)
		.fail(showException);
}

function deleteAllTasks() {
	$.post(allVars.contextPath+"/admin/deleteTasks").fail(showException);
}

function saveAllTasks() {
	$("#saveAllTasksForm").submit();
}

function refreshTaskImportTree() {
	$('#originalAssetsContainer').fileTree(
		allFuncs.treePluginOptions(allVars.contextPath+"/admin/originalAssets", true),
		function($file) {
			allFuncs.selectTreeElement($file, "selectedAssetFile");
		}
	);
}

function addAssetPaths(textures) {
	var pathSep = "&#160;&#160;►&#160;";
	var dir = allVars.selectedAssetFile.attr('rel');
	var $selectedPathsListContainer = $("#selectedPaths");
	var $selectedPathsList = $("#selectedPaths ul");
	$selectedPathsList.empty();
	$.getJSON(allVars.contextPath+"/admin/getAssetPaths",
			{ dir: dir, textures: textures }, function(paths) {
		if(paths.length===0) {
			cancelImport();
			return;
		}
		for(var i in paths) {
			paths[i] = paths[i].replace(new RegExp("/", 'g'), pathSep);
			paths[i] = paths[i].replace(new RegExp("\\\\", 'g'), pathSep);
			$selectedPathsList.append("<li>"+paths[i]+"</li>");
		}
		$selectedPathsListContainer.scrollTop($selectedPathsList.height());
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
	$.post(allVars.contextPath+"/admin/import/cancel")
		.done(function() {hideImport();})
		.fail(showException);
}

function editUserRole(idLink, userRole) {
	var $textField = $("#confirmDialog").find("#confirmDialogTextInput");
	confirm(function () {
		var role = $textField.attr("value");
		$.post(allVars.contextPath+"/admin/users/edit/"+idLink, {"role": role})
			.done(function() {
				window.location.reload();
			})
			.fail(showException);
	}, "Valid roles: ROLE_USER, ROLE_ADMIN", userRole);
}

function switchAdmin(idLink) {
	$.post(allVars.contextPath+"/admin/users/admin/"+idLink)
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
		$.post(allVars.contextPath+"/admin/users/edit/"+idLink, {"name": name})
			.done(function() {
				window.location.reload();
			})
			.fail(showException);
	}, "Enter user name here:", userName);
}

function resetPassword(idLink) {
	confirm(function() {
		$.post(allVars.contextPath+"/admin/users/reset/"+idLink)
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
	$.post(allVars.contextPath+"/admin/users/switch/"+idLink)
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
	$.post(allVars.contextPath+"/admin/users/save").fail(showException);
}

function loadUsers() {
	confirm(function() {
		$.post(allVars.contextPath+"/admin/users/load")
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
	var ResponseBundleOptions = {
			tasks : {
				nextURI : allVars.contextPath+"/admin/load/next",
				conflicts : ["conflict"],
				showButtons : function(conflict, $options) {
					$options.children("#skipButton").show();
					$options.children("#doForAllCheckbox").show();
					$options.children("#overwriteTaskButton").show();
					$options.children("#addBothTasksButton").show();
				},
				start : function() {
					var dir = allVars.selectedBackupFile.attr('rel');
					if(dir === undefined || dir === "") {
						return undefined;
					}
					return $.getJSON(allVars.contextPath+"/admin/load", { dir: dir });
				}

			},
			assets : {
				nextURI : allVars.contextPath+"/admin/importAssets/next",
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
				start : function(assetTypes) {
					var textures;
					if (assetTypes === "textures") textures = true;
					else if (assetTypes === "models") textures = false;
					else return undefined;
					return $("#taskForm").ajaxSubmit({data: { textures: textures }}).data('jqxhr');
				}
			}
		};
	var options = ResponseBundleOptions[responseBundleOption];
	
	var $dialog = $("#bundledMessageDialog");
	var $messageListContainer = $("#messageList");
	var $messageList = $("#messageList ul");
	var $conflictMessage  = $("#conflictMessage");
	var $conflictOptions = $("#conflictOptions");
	var $finishedButton = $("#finishLoadingButton");
	var $checkBox = $('#doForAllCheckbox input');
	
	var that = this;
	
	/**
	 * Get first responses (bundled) from server. Last response in bundle is either either "finish"
	 * or conflict message.
	 */
	this.start = function (startOptions) {
		$conflictOptions.children().hide();
		$finishedButton.hide();
		ajaxResult = options.start(startOptions);
		if (ajaxResult === undefined) return;
		ajaxResult.done(reactToResults).fail(showException);
		showDialogue($dialog);
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
	
	/**
	 * process server response bundle
	 */
	function reactToResults(results) {
		var outOfSync = "Something went wrong! Out of sync with server. Please reload page.";
		//for all but the last element, the status should be success
		for (var i = 0; i < results.length-1; i++) {
			var dataToCheck = results[i];
			if(dataToCheck.status == "success") {
				appendMessage(dataToCheck.message);
			}
			else $conflictMessage.html(outOfSync);
		}
		//last result can be anything
		var data = results[results.length-1];
		if(data.status == "success") {
			//if success, the server can go on with next package
			appendMessage(data.message);
			that.answer("default");
		}
		else if(data.status == "finished") {
			$finishedButton.show();
		}
		else {
			//if conflict, we need to validate the conflict type
			//and show conflict options accordingly.
			$conflictMessage.html(data.message);
			if (options.conflicts.indexOf(data.status) !== -1) {
				options.showButtons(data.status, $conflictOptions);
			}
			else $conflictMessage.html(outOfSync);
		}
		console.log($messageList.height());
		$messageListContainer.scrollTop($messageList.height());
	}
	
	function appendMessage(message) {
		$messageList.append("<li>"+message+"</li>");
	}
}