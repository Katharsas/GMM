function showChangePassword() {
	$("#changePasswordDialog #passwordError").text("");
	showDialogue("#changePasswordDialog");
}

function changePassword() {
	var oldPW = $("#changePasswordDialog #oldPassword").attr("value");
	var newPW1 = $("#changePasswordDialog #newPassword1").attr("value");
	var newPW2 = $("#changePasswordDialog #newPassword2").attr("value");
	
	if(newPW1===newPW2) {
		$.post("profile/password", {"oldPW" : oldPW, "newPW" : newPW2}, function(error) {
			if(error==="") {
				hideDialogue();
			}
			else {
				$("#changePasswordDialog #passwordError").text(error);
			}
		});
	}
	else {
		$("#changePasswordDialog #passwordError").text("Error: New passwords differ!");
	}
}