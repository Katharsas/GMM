import $ from "./lib/jquery";
import Ajax from "./shared/ajax";
import Dialogs from "./shared/dialogs";
import { contextUrl } from "./shared/default";
import {} from "./shared/template";


var ChangePassword = function() {
	var dialogSel = "#changePasswordDialog";
	var $dialog = $(dialogSel);
	
	function showChangePassword($dialog) {
		$dialog.find(dialogSel + "-error").text("");
		Dialogs.showDialog($dialog);
	}
	
	function changePassword() {
		var $error = $dialog.find(dialogSel+"-error");
		
		var oldPW = $dialog.find(dialogSel+"-old").val();
		var newPW1 = $dialog.find(dialogSel+"-first").val();
		var newPW2 = $dialog.find(dialogSel+"-second").val();
		
		if(newPW1 === newPW2) {
			var data = {"oldPW" : oldPW, "newPW" : newPW2};
			Ajax.post(contextUrl + "/profile/password", data)
				.then(function(data) {
					if(data.error === undefined || data.error === null) {
						Dialogs.hideDialog($dialog);
						Dialogs.alert(null, "Password change was successful!");
					} else {
						$error.text(data.error);
					}
				});
		} else {
			$error.text("Error: New passwords differ!");
		}
	}
	
	var $changePWButton = $(".changePasswordButton");
	$changePWButton.on("click", function() {
		showChangePassword($dialog);
	});
	
	var $ok = $dialog.find("#changePasswordDialog-ok");
	$ok.on("click", changePassword);
	
	var $cancel = $dialog.find("#changePasswordDialog-cancel");
	$cancel.on("click", function() {
		Dialogs.hideDialog($dialog);
	});
};


$(document).ready(function() {
	ChangePassword();
});
