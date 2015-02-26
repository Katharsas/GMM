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
	
	var oldPW = $dialog.find(ns+"-old").attr("value");
	var newPW1 = $dialog.find(ns+"-first").attr("value");
	var newPW2 = $dialog.find(ns+"-second").attr("value");
	
	if(newPW1===newPW2) {
		$.post("profile/password", {"oldPW" : oldPW, "newPW" : newPW2})
			.done(function(error) {
				if(error==="") {
					hideDialog($dialog);
					alert(hideDialog, "Password change was successful!");
				} else {
					$error.text(error);
				}
			})
			.fail(showException);
	} else {
		$error.text("Error: New passwords differ!");
	}
}