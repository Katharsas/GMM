

//adds :blank selector to jQuery
$.expr[':'].blank = function(obj) {
	return !$.trim($(obj).text()).length;
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

function htmlDecode(input){
	var e = document.createElement('div');
	e.innerHTML = input;
	return e.childNodes.length === 0 ? "" : e.childNodes[0].nodeValue;
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
	
	hideDialogue();
	//find page tab by URL and set as active tab
	var activeTab = $("#page-tabmenu .tab a[href=\""+allVars.contextPath+"/"+fileName+"\"]").parent();
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
	allVars.$htmlElement.addClass("hideResizeFirefoxFix");
	allVars.$overlay.show();
}

function hideOverlay() {
	allVars.$overlay.hide();
	allVars.$htmlElement.removeClass("hideResizeFirefoxFix");
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

function showDialogue($dialog, width, height) {
	showOverlay();
	centerDialog($dialog, width, height);
	$dialog.show();
}

function hideDialogue($dialog) {
	if ($dialog === undefined) $dialog = $(".dialogContainer");
	$dialog.removeAttr("style");
	$dialog.hide();
	hideOverlay();
}

/**
 * See https://developer.mozilla.org/en-US/docs/Using_files_from_web_applications#Handling_the_upload_process_for_a_file.2C_asynchronously
 * @param file - See HTML 5 File API
 */
function sendFile(file, uri, callback) {
    var xhr = new XMLHttpRequest();
    var fd = new FormData();
    
    xhr.open("POST", uri, true);
    xhr.onreadystatechange = function() {
        if (xhr.readyState == 4 && xhr.status == 200) {
            // Handle response
            callback(xhr.responseText);
        }
    };
    fd.append('myFile', file);
    xhr.send(fd);
}

/**
 * Show a confirmation dialog to the user.
 * @see showConfirmMessage
 */
function confirm(onConfirm, message, textInputDefault, textAreaDefault, width, height) {
	showConfirmDialog(onConfirm, message, true, textInputDefault, textAreaDefault, width, height);
}

/**
 * Show confirmation dialog without cancel button.
 * @see showConfirmMessage
 */
function alert (onConfirm, message, textInputDefault, textAreaDefault) {
	showConfirmDialog(onConfirm, message, false, textInputDefault, textAreaDefault);
}

/**
 * Show a confirmation dialog to the user.
 * 
 * @param onConfirm - Function: callback executes when user hits confirm button (ok).
 * @param message - String: message to show in the dialog.
 * @param hasCancel - boolean: if true, a cancel button will be shown, which closes the dialog.
 * @param textInputDefault - String: If defined, a form input tag will be shown with the argument as its value/text.
 * @param textAreaDefault - String: If defined, a form textarea tag will be shown with the argument as its value/text.
 * @param width - int: Width of the dialog (default is min-width from css).
 * @param height - int: Height of the dialog (default is min-width from css).
 */
function showConfirmDialog(onConfirm, message, hasCancel, textInputDefault, textAreaDefault, width, height) {
	showOverlay();
	//apply elements and texts to dialog
	var $confirmDialog = $("#confirmDialog");
	allVars.onConfirmCallback = function() {
		onConfirm();
		hideDialogue($confirmDialog);
	}
	$confirmDialog.find("#confirmDialogMessage").text(message);
	var $textInputField = $confirmDialog.find("#confirmDialogTextInput");
	var $textArea = $confirmDialog.find("#confirmDialogTextArea");
	var $cancelButton = $confirmDialog.find(".dialogButton.confirmCancel");
	if(textInputDefault !== undefined) {
		$textInputField.attr("value", textInputDefault);
		$textInputField.show();
	}
	else {
		$textInputField.hide();
	}
	if(textAreaDefault !== undefined) {
		$textArea.text(textAreaDefault);
		$textArea.show();
	}
	else {
		$textArea.hide();
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
	if(textInputDefault !== undefined) {
		$textInputField.select();
	}
}

function showException(jqXHR) {
	$(".dialogContainer").hide();
	showOverlay();
	
	var exception = jQuery.parseJSON(jqXHR.responseText);
	var $exceptionDialog = $("#exceptionDialog");
	$exceptionDialog.find("#exceptionMessage").text(exception.message);
	$exceptionDialog.find("#exceptionStackTrace").text(exception.stackTrace);
	showDialogue($exceptionDialog);
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