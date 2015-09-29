/* jshint esnext:true */
import $ from "./lib/jquery";
import Ajax from "./shared/ajax";
import Dialogs from "./shared/dialogs";
import { contextUrl, allVars, allFuncs } from "./shared/default";

$(document).ready( function() {
	hideImport();
	hideTaskFormType();
	refreshTaskBackups();
	refreshTaskImportTree();
	
	var $adminBanner = $("#adminBannerTextArea");
	$adminBanner.html(allVars.adminBanner);
	$adminBanner.blur(function() {
		Ajax.post(contextUrl + "/admin/changeBannerMessage", {message: $adminBanner.val()});
	});
	
	$("#dialog-saveTasks-saveButton").click(saveAllTasks);
});

function refreshTaskBackups() {
	$('#taskBackupsContainer').fileTree(
		allFuncs.treePluginOptions(contextUrl + "/admin/backups", false),
		function($file) {
			allFuncs.selectTreeElement($file, "selectedBackupFile");
		},
		function($dir) {
			var $selected = allVars.selectedBackupFile;
			if ($selected === undefined || $selected.isEmpty()) return;
			if (!$selected.is(':visible')) allVars.selectedBackupFile = $();
		}
	);
}

function hideTaskFormType() {
	$("#taskForm").find("#taskForm-group-type").hide();
}

function loadTasks() {
	var dir = allVars.selectedBackupFile.attr('rel');
	if(dir === undefined || dir === "") return;
	var $confirm = Dialogs.confirm(function() {
		Dialogs.hideDialog($confirm);
		global.ajaxChannel = new ResponseBundleHandler('tasks');
		global.ajaxChannel.start({loadAssets:false, file:dir}, function() {
			refreshTaskBackups();
		});
	}, "Load all tasks from "+dir+"?");
}
global.loadTasks = loadTasks;

function deleteFile() {
	var dir = allVars.selectedBackupFile.attr('rel');
	if(dir === undefined || dir === "") return;
	var $confirm = Dialogs.confirm(function() {
		Ajax.post(contextUrl + "/admin/deleteFile", { dir: dir })
			.done(function() {
				refreshTaskBackups();
				Dialogs.hideDialog($confirm);
			});
	},"Delete file "+dir+"?");
}
global.deleteFile = deleteFile;

function deleteAllTasks() {
	var $confirm = Dialogs.confirm(function() {
		Dialogs.hideDialog($confirm);
		Dialogs.confirm(function() {
			Ajax.post(contextUrl + "/admin/deleteTasks")
				.done(function(){
					refreshTaskBackups();
					Dialogs.hideDialog($confirm);
				});
		}, "Are you really really sure?");
	},"Delete all tasks?");
}
global.deleteAllTasks = deleteAllTasks;

function saveAllTasks() {
	Ajax.post(contextUrl + "/admin/save", {}, $("#dialog-saveTasks-form"))
		.done(function() {
			refreshTaskBackups();
			Dialogs.hideDialog($("#dialog-saveTasks"));
		});
}

function refreshTaskImportTree() {
	$('#originalAssetsContainer').fileTree(
		allFuncs.treePluginOptions(contextUrl + "/admin/originalAssets", true),
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
	var data = { dir: dir, textures: textures };
	Ajax.get(contextUrl + "/admin/getAssetPaths", data, $("form#taskForm"))
		.done(function(paths) {
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
global.addAssetPaths = addAssetPaths;

function hideImport() {
	$("#selectedPaths ul").empty();
	$('#importButtons .button').hide();
	$('#taskForm').hide();
	$('#addMeshesButton').show();
	$('#addTexturesButton').show();
}

function cancelImport() {
	Ajax.post(contextUrl + "/admin/import/cancel")
		.done(function() {hideImport();});
}

function editUserRole(idLink, userRole) {
	var $textField = $("#confirmDialog").find("#confirmDialogTextInput");
	Dialogs.confirm(function () {
		var role = $textField.attr("value");
		Ajax.post(contextUrl + "/admin/users/edit/"+idLink, {"role": role})
			.done(function() {
				window.location.reload();
			});
	}, "Valid roles: ROLE_USER, ROLE_ADMIN", userRole);
}
global.editUserRole = editUserRole;

function switchAdmin(idLink) {
	Ajax.post(contextUrl + "/admin/users/admin/"+idLink)
		.done(function() {
			var $role = $("#"+idLink+" .subElementUserRole");
			if($.trim($role.html())==="[ADMIN]") {
				$role.html("&nbsp;");
			}
			else {
				$role.html("[ADMIN]");
			}
		});
}
global.switchAdmin = switchAdmin;

function editUserName(idLink, userName) {
	Dialogs.confirm(function (name) {
		Ajax.post(contextUrl + "/admin/users/edit/"+idLink, {"name": name})
			.done(function() {
				window.location.reload();
			});
	}, "Enter user name here:", userName);
}
global.editUserName = editUserName;

function resetPassword(idLink) {
	var $confirm = Dialogs.confirm(function() {
		Ajax.post(contextUrl + "/admin/users/reset/" + idLink)
			.done(function(data) {
				Dialogs.hideDialog($confirm);
				var $alert = Dialogs.alert(function() {
					Dialogs.hideDialog($alert);
					window.location.reload();
					}, "New Password:", data[0]);
			});
	}, "Generate New Random Password?");
}
global.resetPassword = resetPassword;

function switchUser(idLink, userName) {
	Ajax.post(contextUrl + "/admin/users/switch/" + idLink)
		.done(function() {
			var $enabled = $("#"+idLink+" .subElementUserEnabled");
			if($.trim($enabled.html()).charCodeAt(0)===0x2611) {
				$enabled.html("&#x2610;");
			}
			else {
				$enabled.html("&#x2611;");
			}
		});
}
global.switchUser = switchUser;

function saveUsers() {
	Ajax.post(contextUrl + "/admin/users/save");
}
global.saveUsers = saveUsers;

function loadUsers() {
	Dialogs.confirm(function() {
		Ajax.post(contextUrl + "/admin/users/load")
			.done(function() {
				window.location.reload();
			});
	}, "Delete all unsaved user data?");
}
global.loadUsers = loadUsers;

function importAssets(assetTypes) {
	global.ajaxChannel = new ResponseBundleHandler("assets");
	global.ajaxChannel.start(assetTypes,
			function() {
		global.ajaxChannel = new ResponseBundleHandler('tasks');
		global.ajaxChannel.start({loadAssets:true});
	});
}
global.importAssets = importAssets;

/**
 * -------------------- ResponseBundleHandler ----------------------------------
 * Provides a way to communicate with the server when the server needs to send
 * a lot of messages to the client and the client needs to be able to respond
 * to any of those messages.
 * 
 * To not waste requests, the messages from the server will come in bundles of
 * dynamic size, where the last bundle message either indicates that the server
 * needs a message from the client or that the server has finished sending all
 * messages or the server wants to give an update because he is very slow.
 * 
 * @author Jan Mothes
 */
function ResponseBundleHandler(responseBundleOption) {
	//Namespace
	var ns = "#batchDialog";
	var ResponseBundleOptions = {
			tasks : {
				nextURI : contextUrl + "/admin/load/next",
				conflicts : ["conflict"],
				showButtons : function(conflict, $options) {
					$options.children(ns+"-skipButton").show();
					$options.children(ns+"-doForAllCheckbox").show();
					$options.children(ns+"-overwriteTaskButton").show();
					$options.children(ns+"-addBothTasksButton").show();
				},
				/**
				 * @param loadAssets:boolean - true if tasks are provided by asset importer,
				 * false if tasks are provided by xml file
				 * @param file:String - xml task file path, if loadAssets is false
				 */
				start : function(options) {
					var data = options.loadAssets ? {} : {dir: options.file};
					return Ajax.post(contextUrl + "/admin/load", data);
				}

			},
			assets : {
				nextURI : contextUrl + "/admin/importAssets/next",
				conflicts : ["taskConflict", "folderConflict"],
				showButtons : function(conflict, $options) {
					$options.children(ns+"-skipButton").show();
					$options.children(ns+"-doForAllCheckbox").show();
					switch(conflict) {
					case "taskConflict":
						$options.children(ns+"-overwriteTaskAquireDataButton").show();
						$options.children(ns+"-overwriteTaskDeleteDataButton").show();
						break;
					case "folderConflict":
						$options.children(ns+"-aquireDataButton").show();
						$options.children(ns+"-deleteDataButton").show();
						break;
					}
				},
				/**
				 * @param assetType:String - "textures" or "models"
				 */
				start : function(assetType) {
					var textures;
					if (assetType === "textures") textures = true;
					else if (assetType === "models") textures = false;
					else return undefined;
					return Ajax.post(contextUrl + "/admin/importAssets", { textures: textures }, $("#taskForm"));
				}
			}
		};
	var options = ResponseBundleOptions[responseBundleOption];
	
	var $dialog = $(ns);
	var $messageListContainer =
			$dialog.find(ns+"-listWrapper");
	var $messageList =
			$dialog.find(ns+"-list");
	var $conflictMessage  =
			$dialog.find(ns+"-conflictMessage");
	var $conflictOptions =
			$dialog.find(ns+"-conflictOptions");
	var $finishedButton =
			$dialog.find(ns+"-finishLoadingButton");
	var $checkBox =
			$dialog.find(ns+'-doForAllCheckbox input');
	
	var callback;
	var that = this;
	
	/**
	 * Get first responses (bundled) from server. Last response in bundle is either either "finish"
	 * or conflict message.
	 * @param startOptions:Any - see start method from chosen ResponseBundleOption
	 * @param onFinished:Function - callback
	 */
	this.start = function (startOptions, onFinished) {
		callback = onFinished;
		$conflictOptions.children().hide();
		$finishedButton.hide();
		var ajaxResult = options.start(startOptions);
		if (ajaxResult === undefined) return;
		ajaxResult.done(reactToResults);
		Dialogs.showDialog($dialog);
	};
	
	/**
	 * If a conflict occured at last bundle, use this method to give an answer for conflict handling.
	 * Server will return another response bundle.
	 */
	this.answer = function (answer) {
		$conflictOptions.children().hide();
		$conflictMessage.empty();
		var doForAll = $checkBox.is(":checked");
		Ajax.post(options.nextURI, { operation: answer, doForAll: doForAll })
			.done(reactToResults);
	};
	
	this.finish = function () {
		Dialogs.hideDialog($dialog);
		$messageList.empty();
		if (callback !== undefined) callback();
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
		$messageListContainer.scrollTop($messageList.height());
	}
	
	function appendMessage(message) {
		$messageList.append("<li>"+message+"</li>");
	}
}