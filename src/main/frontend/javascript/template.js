import Ajax from "./shared/ajax";
import HtmlPreProcessor from "./shared/preprocessor";
import Notifications from "./shared/notifications";
import { contextUrl, allVars, htmlDecode } from "./shared/default";// must import to execute default

var fileName = window.location.pathname.substr(window.location.pathname.lastIndexOf("/")+1);

/**
 * This function is executed when document is ready for interactivity!
 */
$(document).ready(function() {
	//find page tab by URL and set as active tab
	var activeTab = $("#page-tabmenu .tab a[href=\""+ contextUrl +"/"+fileName+"\"]").parent();
	activeTab.addClass("activeTab activePage");
	
	$("#page-tabmenu #logout").click(function()  {
		Ajax.post( contextUrl + "/logout")
			.then(function() {
				window.location.reload();
			});
	});
	
	allVars.adminBanner = htmlDecode(allVars.adminBanner);
	var $adminBanner = $("#customAdminBanner");
	if ($adminBanner.length > 0) {
		var doubleDecoded = htmlDecode(allVars.adminBanner);
		$adminBanner.html(doubleDecoded);
	}
	
	HtmlPreProcessor.apply($("body"));

	Notifications.init();
});