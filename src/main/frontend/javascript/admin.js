import $ from "./lib/jquery";
import Ajax from "./shared/ajax";
import Dialogs from "./shared/dialogs";
import ResponseBundleHandler from "./shared/responseBundleHandler";
import { contextUrl, allVars, allFuncs } from "./shared/default";

var Database = function() {
	var $database = $("#database");
	
	/**
	 * Refreshes the file tree showing task save files.
	 */
	var refreshDatabaseFileTree = function() {
		$database.find("#database-fileTreeContainer").fileTree(
			{url: contextUrl + "/admin/backups"},
			function($file) {
				allFuncs.selectTreeElement($file, "selectedBackupFile");
			},
			function($dir) {
				var $selected = allVars.selectedBackupFile;
				if ($selected === undefined || $selected.isEmpty()) return;
				if (!$selected.is(':visible')) allVars.selectedBackupFile = $();
			}
		);
	};
	
	//load file
	$database.find("#database-loadFile").click(function() {
		var dir = allVars.selectedBackupFile.attr('rel');
		if(dir === undefined || dir === "") return;
		var $confirm = Dialogs.confirm(function() {
			Dialogs.hideDialog($confirm);
			var url = contextUrl + "/admin/load/assetPaths";
			global.ajaxChannel = new ResponseBundleHandler(url, "assets");
			global.ajaxChannel.start({file:dir}, function() {
				var url = contextUrl + "/admin/load/tasks";
				global.ajaxChannel = new ResponseBundleHandler(url, "tasks");
				global.ajaxChannel.start({});
			});
		}, "Load all tasks from "+dir+"?");
	});
	
	//delete file
	$database.find("#database-deleteFile").click(function() {
		var dir = allVars.selectedBackupFile.attr('rel');
		if(dir === undefined || dir === "") return;
		var $confirm = Dialogs.confirm(function() {
			Ajax.post(contextUrl + "/admin/deleteFile", { dir: dir })
				.then(function() {
					refreshDatabaseFileTree();
					Dialogs.hideDialog($confirm);
				});
		},"Delete file "+dir+"?");
	});
	
	//save all tasks
	$database.find("#database-saveAll").click(function() {
		Dialogs.showDialog($('#dialog-saveTasks'));
	});
	$("#dialog-saveTasks-saveButton").click(function() {
		Ajax.post(contextUrl + "/admin/save", {}, $("#dialog-saveTasks-form"))
			.then(function() {
				refreshDatabaseFileTree();
				Dialogs.hideDialog($("#dialog-saveTasks"));
			});
	});
	
	//delete all tasks
	$database.find("#database-deleteAll").click(function() {
		var $confirm = Dialogs.confirm(function() {
			Dialogs.hideDialog($confirm);
			var $confirm2 = Dialogs.confirm(function() {
				Ajax.post(contextUrl + "/admin/deleteTasks")
					.then(function(){
						refreshDatabaseFileTree();
						Dialogs.hideDialog($confirm2);
					});
			}, "Are you really really sure?");
		},"Delete all tasks?");
	});
	
	refreshDatabaseFileTree();
};

var AssetImport = function() {
	var $assets = $("#assets");
	
	$assets.find("#taskForm").find("#taskForm-group-type").hide();
	
	$assets.find('#originalAssets-fileTreeContainer').fileTree(
		{url: contextUrl + "/admin/originalAssets"},
		function($file) {
			allFuncs.selectTreeElement($file, "selectedAssetFile");
		}
	);

	$assets.find('#newAssets-fileTreeContainer').fileTree(
		{url: contextUrl + "/admin/newAssets"},
		function($file) {
			allFuncs.selectTreeElement($file, "selectedAssetFile");
		}
	);

	allVars.activeFileTree = null;
};

$(document).ready( function() {
	hideImport();
	
	var $adminBanner = $("#adminBannerTextArea");
	$adminBanner.html(allVars.adminBanner);
	$adminBanner.blur(function() {
		Ajax.post(contextUrl + "/admin/changeBannerMessage", {message: $adminBanner.val()});
	});
	
	Database();
	AssetImport();
	UserManager();
});

function addAssetPaths(textures) {

	var $fileTreeContainer = allVars.selectedAssetFile.closest(".fileTreeContainer");
	var fileTreeId = $fileTreeContainer.attr('id');
	if (allVars.activeFileTree !== null && allVars.activeFileTree !== fileTreeId) {
		cancelImport().then(addSelectedAssetPaths);
	} else {
		addSelectedAssetPaths();
	}
	allVars.activeFileTree = fileTreeId;

	function addSelectedAssetPaths() {
		var pathSep = "&#160;&#160;►&#160;";
		var dir = allVars.selectedAssetFile.attr('rel');
		var $selectedPathsListContainer = $("#selectedPaths");
		var $selectedPathsList = $("#selectedPaths ul");
		$selectedPathsList.empty();

		var data = { dir: dir, textures: textures, isOriginal: fileTreeId.startsWith("original") };
		Ajax.get(contextUrl + "/admin/getAssetPaths", data, $("form#taskForm"))
			.then(function(paths) {
				if(paths.length===0) {
					cancelImport();
					return;
				}
				for(var path of paths) {
					path = path.replace(new RegExp("/", 'g'), pathSep);
					path = path.replace(new RegExp("\\\\", 'g'), pathSep);
					$selectedPathsList.append("<li>"+path+"</li>");
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
	return Ajax.post(contextUrl + "/admin/import/cancel")
		.then(function() {
			hideImport();
			allVars.activeFileTree = null;
		});
}
global.cancelImport = cancelImport;

/**
 * -------------------- UserManaer --------------------------------------------------------------
 * Registers event listeners for user list and buttons.
 */
var UserManager = function() {
	var $users = $("#admin-users");
	
	var callWithUser = function($child, callback) {
		var $user = $child.closest(".admin-user");
		var idLink = $user.attr("id");
		callback($user, idLink, $child);
	};
	// single user
	//-----------------------------------------------------------------
	var switchUser = function($user, idLink, $enabled) {
		Ajax.post(contextUrl + "/admin/users/switch/" + idLink)
			.then(function() {
				if($.trim($enabled.html()).charCodeAt(0)===0x2611) {
					$user.addClass("disabled");
					$enabled.html("&#x2610;");
				}
				else {
					$user.removeClass("disabled");
					$enabled.html("&#x2611;");
				}
			});
	};
	$users.on("click", ".admin-user-enabled", function() {
		var $child = $(this);
		callWithUser($child, switchUser);
	});
	//-----------------------------------------------------------------
	var switchAdmin = function($user, idLink, $role) {
		Ajax.post(contextUrl + "/admin/users/admin/" + idLink)
			.then(function() {
				if($.trim($role.html())==="[ADMIN]") {
					$role.html("&nbsp;");
				}
				else {
					$role.html("[ADMIN]");
				}
			});
	};
	$users.on("click", ".admin-user-role", function() {
		var $child = $(this);
		callWithUser($child, switchAdmin);
	});
	//-----------------------------------------------------------------
	var editUserName = function($user, idLink) {
		var userName = $user === null ? "" : $user.find(".admin-user-name").attr("data-name");
		Dialogs.confirm(function (changedName) {
			Ajax.post(contextUrl + "/admin/users/edit/"+idLink, {"name": changedName})
				.then(function() {
					window.location.reload();
				});
		}, "Enter user name here:", userName);
	};
	$users.on("click", ".admin-user-buttonRename", function() {
		var $child = $(this);
		callWithUser($child, editUserName);
	});
	//-----------------------------------------------------------------
	var resetPassword = function($user, idLink) {
		var $confirm = Dialogs.confirm(function() {
			Ajax.post(contextUrl + "/admin/users/reset/" + idLink)
				.then(function(data) {
					Dialogs.hideDialog($confirm);
					var $alert = Dialogs.alert(function() {
						Dialogs.hideDialog($alert);
						window.location.reload();
						}, "New Password:", data[0]);
				});
		}, "Generate New Random Password?");
	};
	$users.on("click", ".admin-user-buttonReset", function() {
		var $child = $(this);
		callWithUser($child, resetPassword);
	});
	
	// all users buttons
	//-----------------------------------------------------------------
	$(".admin-users-new").on("click", function() {
		editUserName(null, "new");
	});
	$(".admin-users-save").on("click", function() {
		Ajax.post(contextUrl + "/admin/users/save")
			.then(function() {
				Dialogs.alert(null, "Users saved!");
			});
	});
	$(".admin-users-load").on("click", function() {
		Dialogs.confirm(function() {
			Ajax.post(contextUrl + "/admin/users/load")
				.then(function() {
					window.location.reload();
				});
		}, "Delete all unsaved user data?");
	});
};



function importAssets() {
	var url = contextUrl + "/admin/importAssets";
	var ajaxChannel = new ResponseBundleHandler(url, "assets");
	ajaxChannel.start({$taskForm: $("#taskForm")});
}
global.importAssets = importAssets;
