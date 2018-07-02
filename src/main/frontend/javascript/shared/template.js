import $ from "../lib/jquery";
import Ajax from "./ajax";
import HtmlPreProcessor from "./preprocessor";
import { contextUrl, allVars, htmlDecode } from "./default";// must import to execute default
import AssetFileOperationsNotifier, { AssetFileOperationsNotifierInit } from "./AssetFileOperationsNotifier";
import EventListener from "./EventListener";

global.Promise.onPossiblyUnhandledRejection(function(error) {
    throw error;
});

var pageName = window.location.pathname.substr(window.location.pathname.lastIndexOf("/")+1);

/**
 * This function needs to be executed when document is ready for interactivity!
 */
$(document).ready(function() {

	var $menu = $("#page-tabmenu");


	HtmlPreProcessor.apply($("body")).then(function() {

		var $menuLoading = $menu.find("#loading");

		Ajax.registerOnSend(function(requests) {
			if (requests <= 0) {
				var menuLoadingSVG = $menuLoading[0].getElementsByTagName("svg")[0];
				menuLoadingSVG.setCurrentTime(0);
				$menuLoading.addClass("active");
			}
		});
		Ajax.registerOnReceive(function(requests) {
			if (requests <= 0) {
				$menuLoading.removeClass("active");
			}
		});
	});

	//find page tab by URL and set as active tab
	var activeTab = $menu.find(".tab a[href=\""+ contextUrl +"/"+pageName+"\"]").parent();
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
	
	// init asset file operations icon
	if (allVars.isUserLoggedIn) {
		AssetFileOperationsNotifierInit("/tasks/newAssetFileOperationsEnabled");
		const $icon = $("#page-tabmenu #assetOperations");
		AssetFileOperationsNotifier.registerSubscriber(function(noAssetOperations) {
			if (noAssetOperations) {
				$icon.removeClass("active");
			} else {
				$icon.addClass("active");
			}
		}, "statusIcon");
	}
});