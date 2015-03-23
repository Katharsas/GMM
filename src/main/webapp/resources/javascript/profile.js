function showChangePassword() {
	var $dialog = $("#changePasswordDialog");
	$dialog.find("#changePasswordDialog-error").text("");
	showDialog($dialog);
}

function changePassword() {
	//Namespace
	var ns = "#changePasswordDialog";
	var $dialog = $(ns);
	var $error = $dialog.find(ns+"-error");
	
	var oldPW = $dialog.find(ns+"-old").val();
	var newPW1 = $dialog.find(ns+"-first").val();
	var newPW2 = $dialog.find(ns+"-second").val();
	
	if(newPW1 === newPW2) {
		var data = {"oldPW" : oldPW, "newPW" : newPW2};
		Ajax.post(contextUrl + "/profile/password", data)
			.done(function(data) {
				if(data.error === undefined || data.error === null) {
					hideDialog($dialog);
					alert(hideDialog, "Password change was successful!");
				} else {
					$error.text(data.error);
				}
			});
	} else {
		$error.text("Error: New passwords differ!");
	}
}