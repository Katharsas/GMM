

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
	"selectedTaskFile":$(),
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
	
	hideDialog();
	//find page tab by URL and set as active tab
	var activeTab = $("#page-tabmenu .tab a[href=\""+ contextUrl +"/"+fileName+"\"]").parent();
	activeTab.addClass("activeTab activePage");
	
	//setup enter keys of dialogs
	$(".dialogContainer").bind("keypress", function(event) {
		if(event.which === 13) {
			confirmOk();
		}
	});
	$(".draggable").fixedDraggable();
	
	allVars.adminBanner = htmlDecode(allVars.adminBanner);
	var $adminBanner = $("#customAdminBanner");
	if ($adminBanner.length > 0) {
		var doubleDecoded = htmlDecode(allVars.adminBanner);
		$adminBanner.html(doubleDecoded);
	}
	
	allVars.htmlPreProcessor = HtmlPreProcessor();
	allVars.htmlPreProcessor.apply($("body"));
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

function showOverlay() {
	allVars.$overlay.show();
}

function hideOverlay() {
	allVars.$overlay.hide();
}

/**
 * @param width - int: Width of the dialog (default is min-width from css).
 * @param height - int: Height of the dialog (default is min-width from css).
 */
function centerDialog($dialog, width, height) {
	if(width === undefined)  width = $dialog.outerWidth();
	$dialog.css("left", ($(window).innerWidth()/2-width/2)+"px");
	if(height === undefined) height = $dialog.innerHeight();
	$dialog.css("top", (($(window).innerHeight()/2-height/2)*0.7)+"px");
}

function setDialogDimensions($dialog, width, height) {
	if(width === undefined)  width = $dialog.outerWidth();
	$dialog.css("min-width", width+"px");
	if(height === undefined) height = $dialog.innerHeight();
	$dialog.css("min-height", height+"px");
}

function showDialog($dialog, width, height) {
	showOverlay();
	centerDialog($dialog, width, height);
	$dialog.show();
}

function hideDialog($dialog) {
	if ($dialog === undefined) $dialog = $(".dialog");
	if (!$dialog.hasClass("dialog")) $dialog = $dialog.parents(".dialog");
	$dialog.removeAttr("style");
	$dialog.hide();
	hideOverlay();
}

/**
 * Show a confirmation dialog to the user.
 * @see showConfirmMessage
 */
function confirm(onConfirm, message, textInputDefault, textAreaDefault, width, height) {
	return showConfirmDialog(onConfirm, message, true, textInputDefault, textAreaDefault, width, height);
}

/**
 * Show confirmation dialog without cancel button.
 * @see showConfirmMessage
 */
function alert (onConfirm, message, textInputDefault, textAreaDefault) {
	var $dialog = showConfirmDialog(
		function(){
			hideDialog($dialog);
			onConfirm();
		}, message, false, textInputDefault, textAreaDefault);
	return $dialog;
}

/**
 * Show a confirmation dialog to the user.
 * 
 * @param onConfirm - Function: callback executes when user hits confirm button (ok).
 * @param message - String: message to show in the dialog.
 * @param hasCancel - boolean: if true, a cancel button will be shown, which closes the dialog.
 * @param inputDefault - String: If defined, a form input tag will be shown with the argument as its value/text.
 * @param textareaDefault - String: If defined, a form textarea tag will be shown with the argument as its value/text.
 * @param width - int: Width of the dialog (default is min-width from css).
 * @param height - int: Height of the dialog (default is min-width from css).
 */
function showConfirmDialog(onConfirm, message, hasCancel, inputDefault, textareaDefault, width, height) {
	showOverlay();
	//apply elements and texts to dialog
	var $confirmDialog = $("#confirmDialog");
	$confirmDialog.find("#confirmDialog-message").text(message);
	var $input = $confirmDialog.find("#confirmDialog-input");
	var $textarea = $confirmDialog.find("#confirmDialog-textarea");
	allVars.onConfirmCallback = function() {
		onConfirm($input.val(), $textarea.val());
	};
	var $cancelButton = $confirmDialog.find("#confirmDialog-cancel");
	if(inputDefault !== undefined) {
		$input.attr("value", inputDefault);
		$input.show();
	}
	else {
		$input.hide();
	}
	if(textareaDefault !== undefined) {
		$textarea.text(textareaDefault);
		$textarea.show();
	}
	else {
		$textarea.hide();
	}
	if(hasCancel) {
		$cancelButton.show();
	}
	else {
		$cancelButton.hide();
	}
	//set width and height & center
	setDialogDimensions($confirmDialog, width, height);
	centerDialog($confirmDialog, width, height);
	//show
	$confirmDialog.show();
	if(inputDefault !== undefined) {
		$input.select();
	}
	return $confirmDialog;
}

function showException(jqXHR) {
	hideDialog();
	showOverlay();
	
	var exception = jQuery.parseJSON(jqXHR.responseText);
	var $exceptionDialog = $("#exceptionDialog");
	$exceptionDialog.find("#exceptionDialog-message").text(exception.message);
	$exceptionDialog.find("#exceptionDialog-trace").text(exception.stackTrace);
	showDialog($exceptionDialog);
}

function confirmOk() {
	allVars.onConfirmCallback();
}

/**
 * Don't worry about this one.
 */
function testAlert(string) {
	alert('Test Alert! '+string);
}