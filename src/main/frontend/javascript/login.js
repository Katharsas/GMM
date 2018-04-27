import $ from "./lib/jquery";
import {} from "./shared/template";

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