import $ from "./lib/jquery";

$(document).ready(function() {
	var $submit = $("#login-form-submit");
	$submit.click(function() {
		$('#loginForm').submit();
	});
	
	var $pw = $("#login-form-password");
	$pw.onEnter(function() {
		$submit.click();
	});
});