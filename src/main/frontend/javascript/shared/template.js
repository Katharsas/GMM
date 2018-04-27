import $ from "../lib/jquery";
import Ajax from "./ajax";
import HtmlPreProcessor from "./preprocessor";
import { contextUrl, allVars, htmlDecode } from "./default";// must import to execute default
import EventListener from "./EventListener";

var pageName = window.location.pathname.substr(window.location.pathname.lastIndexOf("/")+1);

/**
 * This function needs to be executed when document is ready for interactivity!
 */
$(document).ready(function() {
	//find page tab by URL and set as active tab
	var activeTab = $("#page-tabmenu .tab a[href=\""+ contextUrl +"/"+pageName+"\"]").parent();
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

	console.log("Subscribing to import events");
	EventListener.subscribe(EventListener.events.AssetImportRunningEvent, function() {
		console.log("Received AssetImportRunningEvent!");
	})
});