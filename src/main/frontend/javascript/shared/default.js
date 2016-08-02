import "./jquery/jqueryFileTree";
import "./jquery/jqueryDraggable";
import "./jquery/jqueryFindSelf";
import "./dialogs";

import $ from "../lib/jquery";
import Ajax from "./ajax";
import HtmlPreProcessor from "./preprocessor";
import Errors from "./Errors";

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

allVars.adminBanner = global.templateVars.adminBanner;
allVars.isUserLoggedIn = global.templateVars.isUserLoggedIn;
allVars.currentUser = !allVars.isUserLoggedIn ? null : {
	idLink : global.templateVars.userIdLink,
	name : global.templateVars.userName
};
global.allVars = allVars;//TODO: remove it unused from html

var contextUrl = global.contextUrl;

String.prototype.nl2br = function(is_xhtml) {   
    var breakTag = (is_xhtml || typeof is_xhtml === 'undefined') ? '<br />' : '<br>';    
    return (this + '').replace(/([^>\r\n]?)(\r\n|\n\r|\r|\n)/g, '$1'+ breakTag +'$2');
};
Array.prototype.diff = function(a) {
    return this.filter(function(i) {return a.indexOf(i) < 0;});
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

/**
 * @callback getIdOfElement
 * @param {Element} element - element from which id needs to be extracted
 * @returns {Object} id - if of the given element
 */
/**
 * Resorts a list of dom elements based on another list.
 * @param {Object[]} orderedIds - array of ids with order used to order dom elements.
 * @param {jquery} $unorderedList - parent of dom elements which are to be ordered.
 * @param {string} selector - allows to filter children of $unorderedList
 * @param {getIdOfElement} getIdOfElement - Callback for extracting ids from list elements.
 * 		Those ids will be compared with the ids of the orderedIds array.
 */
function resortElementsById(orderedIds, $unorderedList, selector, getIdOfElement) {
	var $unorderedElements = $unorderedList.children(selector);
	if (orderedIds.length !== $unorderedElements.length) {
		throw new Errors.IllegalArgumentError("List lengths do not match!");
	}
	var idToIndexOrdered = {};
	for(var i = 0; i < orderedIds.length; i++) {
		idToIndexOrdered[orderedIds[i]] = i;
	}
	$unorderedElements.detach().sort(function(element1, element2) {
		var id1 = getIdOfElement(element1);
		var id2 = getIdOfElement(element2);
		return idToIndexOrdered[id1] - idToIndexOrdered[id2];
	});
	$unorderedList.append($unorderedElements);
}

/**
 * Async task returning a promise.
 * @callback PromiseProducer
 * @return {Promise} - Will be resolved when task has finished.
 */
/**
 * Executes multiple async functions so that every function only gets executed
 * when the previous functions has resolved. Returns the last functions promise.
 * @param {PromiseProducer}
 */
function runSerial(tasks) {
	var promiseChain = Promise.resolve();
	for (var task of tasks) {
		promiseChain = promiseChain.then(task);
	}
	return promiseChain;
}

export { contextUrl, allVars, allFuncs, htmlDecode, getURLParameter, resortElementsById, runSerial };
export default {};