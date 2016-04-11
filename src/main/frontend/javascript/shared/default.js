import "./jqueryFileTree";
import "./jqueryDraggable";

import $ from "../lib/jquery";
import Ajax from "./ajax";
import Dialogs from "./dialogs";
import HtmlPreProcessor from "./preprocessor";

//adds :blank selector to jQuery
$.expr[':'].blank = function(obj) {
	return !$.trim($(obj).text()).length;
};
//adds isEmpty function to jQuery
$.fn.isEmpty = function() {
	return this.length < 1;
};
$.fn.onEnter = function(eventHandler) {
	this.keypress(function(event) {
		if (event.keyCode === 13) eventHandler(event);
	});
};

var fileName = window.location.pathname.substr(window.location.pathname.lastIndexOf("/")+1);
var paramString = window.location.search.substring(1);

var allVars = {
	"selectedAssetFile":$(),
	"selectedBackupFile":$(),
	"taskBackgroundColor":"#111",
	"$htmlElement":$("html")
};

var allFuncs = {
	"selectTreeElement":
		function($newFile, marker) {
			var $oldFile = allVars[marker];
			$oldFile.removeClass(marker);
			$newFile.addClass(marker);
			allVars[marker] = $newFile;
		},
	"treePluginOptions":
		function(mapping, directories) {
			return {
				root : "",
				script : mapping,
				expandSpeed : 300,
				collapseSpeed : 300,
				directoryClickable : directories
			};
		}
};

allVars.adminBanner = global.allVars.adminBanner;
global.allVars = allVars;//TODO: remove it unused from html

var contextUrl = global.contextUrl;//TODO: remove if unused in HTML

String.prototype.nl2br = function(is_xhtml) {   
    var breakTag = (is_xhtml || typeof is_xhtml === 'undefined') ? '<br />' : '<br>';    
    return (this + '').replace(/([^>\r\n]?)(\r\n|\n\r|\r|\n)/g, '$1'+ breakTag +'$2');
};

/**
 * Unescapes & trims html text to javascript text (including convertion of <br> tags to newLine).
 */
function htmlDecode(input){
	var e = document.createElement('div');
	e.innerHTML = input;
	var result = "";
	for (var i = 0; i < e.childNodes.length; i++) {
		//if type = text, add text
		if (e.childNodes[i].nodeType === 3) {
			result += e.childNodes[i].nodeValue;
		//if type = node, check for <br> node
		} else if (e.childNodes[i].nodeType === 1) {
			if (e.childNodes[i].tagName === "BR") {
				result += "\n";
			}
		}
	}
	result = result.replace(/\s\s+/g, ' ');
	return result.trim();
}

/*
 * ////////////////////////////////////////////////////////////////////////////////
 * FUNCTIONS
 * ////////////////////////////////////////////////////////////////////////////////
 */

/**
 * This function is executed when document is ready for interactivity!
 */
$(document).ready(function() {
	allVars.$overlay = $("#overlay");
	
	Dialogs.hideDialog();
	//find page tab by URL and set as active tab
	var activeTab = $("#page-tabmenu .tab a[href=\""+ contextUrl +"/"+fileName+"\"]").parent();
	activeTab.addClass("activeTab activePage");
	
	//setup enter keys of dialogs
	$(".dialogContainer").bind("keypress", function(event) {
		if(event.which === 13) {
			Dialogs.confirmOk();
		}
	});
	$(".draggable").fixedDraggable();
	
	$("#page-tabmenu #logout").click(function()  {
		Ajax.post( contextUrl + "/logout")
			.done(function() {
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
});


/*
 * Get value of URL parameter "sParam"
 */
function getURLParameter(sParam)
{
    var sURLVariables = paramString.split('&');
    for (var i = 0; i < sURLVariables.length; i++)
    {
        var sParameterName = sURLVariables[i].split('=');
        if (sParameterName[0] == sParam)
        {
            return sParameterName[1];
        }
    }
    return "";
}

export { contextUrl, allVars, allFuncs, htmlDecode, getURLParameter };
export default {};