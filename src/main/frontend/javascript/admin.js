import $ from "./lib/jquery";
import Ajax from "./shared/ajax";
import Dialogs from "./shared/dialogs";
import ResponseBundleHandler from "./shared/responseBundleHandler";
import { contextUrl, allVars, allFuncs } from "./shared/default";
import {} from "./shared/template";

$(document).ready( function() {
	
	var $adminBanner = $("#adminBannerTextArea");
	$adminBanner.html(allVars.adminBanner);
	$adminBanner.blur(function() {
		Ajax.post(contextUrl + "/admin/changeBannerMessage", {message: $adminBanner.val()});
	});
	
	Database();
	AssetImport();
	UserManager();
});

/**
 * -------------------- DataBase -----------------------------------------------------------------
 * Create filetree, registers event listeners for saving/loading/deleting backups/tasks.
 */
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
	
	//save all tasks as
	$database.find("#database-saveAllAs").click(function() {
		Dialogs.showDialog($('#dialog-saveTasks'));
	});
	$("#dialog-saveTasks-saveButton").click(function() {
		Ajax.post(contextUrl + "/admin/save", {}, $("#dialog-saveTasks-form"))
			.then(function() {
				refreshDatabaseFileTree();
				Dialogs.hideDialog($("#dialog-saveTasks"));
			});
	});

	//save all tasks
	$database.find("#database-saveAll").click(function() {
		Ajax.post(contextUrl + "/admin/save", {})
			.then(function() {
				refreshDatabaseFileTree();
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

/**
 * -------------------- AssetImport --------------------------------------------------------------
 * Create filetrees, registers event listeners for adding files & import.
 */
var AssetImport = function() {
	var $assets = $("#assets");
	
	$assets.find("#taskForm").find("#taskForm-group-type").hide();
	
	var hideImport = function() {
		$assets.find("#selectedPaths ul").empty();
		$assets.find('#importButtons .button').hide();
		$assets.find('#taskForm').hide();
	};
	hideImport();

	// Auto import
	var $autoImportInput = $assets.find("#autoImportInput");
	$autoImportInput.on("change", function(event) {
		const isEnabled = $(this).is(':checked');
		const ajaxCall = function() {
			return Ajax.post(contextUrl + "/admin/autoImport", {"isEnabled": isEnabled});
		}
		if (isEnabled) {
			const dialog = Dialogs.confirm(function() {
				ajaxCall().then(function() {
					window.location.reload();
				});
			}, "Enabling will trigger an initial new asset import. Continue?");
		} else {
			ajaxCall();
		}
	});
	
	// FileTrees
	
	var $originalFileTree = $assets.find('#originalAssets-fileTreeContainer');
	var $newFileTree = $assets.find('#newAssets-fileTreeContainer');
	
	var updateFileTreeOriginal = function() {
		$originalFileTree.fileTree(
			{url: contextUrl + "/admin/originalAssets"},
			function($file) {
				allFuncs.selectTreeElement($file, "selectedAssetFile");
			}
		);
	};
	var updateFileTreeNew = function() {
		$newFileTree.fileTree(
			{url: contextUrl + "/admin/newAssets"},
			function($file) {
				allFuncs.selectTreeElement($file, "selectedAssetFile");
			}
		);
	};

	updateFileTreeOriginal();
	updateFileTreeNew();
	allVars.activeFileTree = null;
	
	// add asset paths
	
	var addAssetPaths = function() {
		
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
			
			var data = { dir: dir, isOriginal: fileTreeId.startsWith("original") };
			Ajax.get(contextUrl + "/admin/getAssetPaths", data, $("form#taskForm"))
				.then(function(paths) {
					if(paths.length === 0) {
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
			
			$assets.find('#importButtons .button').show();
			$assets.find('#taskForm').show();
		}
	};
	
	$assets.find("#addAssetsButton").click(function() {
		addAssetPaths();
	});
	
	// Import Buttons
	
	var importAssets = function(callback) {
		var url = contextUrl + "/admin/importAssets";
		var ajaxChannel = new ResponseBundleHandler(url, "assets");
		ajaxChannel.start({$taskForm: $("#taskForm")}, function() {
			if (allVars.activeFileTree === $originalFileTree.attr('id')) {
				updateFileTreeOriginal();
			} else if (allVars.activeFileTree === $newFileTree.attr('id')) {
				updateFileTreeNew();
			}
			// TODO hideImport? (like cancelimport)?
		});
	};
	
	var cancelImport = function() {
		return Ajax.post(contextUrl + "/admin/import/cancel")
			.then(function() {
				hideImport();
				allVars.activeFileTree = null;
			});
	};
	
	var $buttons = $assets.find("#importButtons");
	$buttons.find("#importAssetsButton").click(function() {
		importAssets();
	});
	$buttons.find("#cancelImportButton").click(function() {
		cancelImport();
	});
};

/**
 * -------------------- UserManager --------------------------------------------------------------
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
					Dialogs.alert(function() {
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