var fileName = window.location.pathname.substr(window.location.pathname.lastIndexOf("/")+1);
var paramString = window.location.search.substring(1);


var allVars = {
	"selectedTaskFile":$(),
	"selectedAssetFile":$(),
	"selectedBackupFile":$()
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

/*
 * ////////////////////////////////////////////////////////////////////////////////
 * FUNCTIONS
 * ////////////////////////////////////////////////////////////////////////////////
 */

/**
 * This function is executed when document is ready for interactivity!
 */
$(document).ready(function() {
	hideDialogue();
	//find page tab by URL and set as active tab
	var activeTab = $(".pageTabmenu .tab a[href=\""+fileName+"\"]").parent();
	activeTab.addClass("activeTab activePage");
	
	//setup enter keys
	$(".dialogContainer").bind("keypress", function(event) {
		if(event.which === 13) {
			confirmOk();
		}
	});
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

function showDialogue(selector) {
	$("#overlay").show();
	$(selector).show();
}

function hideDialogue() {
	$(".dialogContainer").hide();
	$("#overlay").hide();
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
function confirm(onConfirm, message, textInputDefault, textAreaDefault) {
	showConfirmDialog(onConfirm, message, true, textInputDefault, textAreaDefault);
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
 * @param textInputDefault - String: If defined, a form input tag will be shown. Default text value of the input.
 * @param textAreaDefault - String: If defined, a form textarea tag will be shown. Default text value of the textarea.
 */
function showConfirmDialog(onConfirm, message, hasCancel, textInputDefault, textAreaDefault) {
	allVars.onConfirmCallback = onConfirm;
	var $confirmDialog = $("#confirmDialog");
	$confirmDialog.find("#confirmDialogMessage").text(message);
	var $textInputField = $confirmDialog.find("#confirmDialogTextInput");
	var $textArea = $confirmDialog.find("#confirmDialogTextArea");
	var $cancelButton = $confirmDialog.find(".dialogButton.confirmCancel");
//	var $okButton = $confirmDialog.find(".dialogButton.confirmOk");
	if(textInputDefault !== undefined) {
		$textInputField.attr("value", textInputDefault);
		$textInputField.show();
	}
	else {
		$textInputField.hide();
	}
	if(textAreaDefault !== undefined) {
		$textArea.attr("value", textAreaDefault);
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
	$("#overlay").show();
	$confirmDialog.show();
	if(textInputDefault !== undefined) {
		$textInputField.select();
	}
}

function showException(jqXHR) {
	$(".dialogContainer").hide();
	$("#overlay").show();
	
	var exception = jQuery.parseJSON(jqXHR.responseText);
	var $exceptionDialog = $("#exceptionDialog");
	$exceptionDialog.find("#exceptionMessage").text(exception.message);
	$exceptionDialog.find("#exceptionStackTrace").text(exception.stackTrace);
	$exceptionDialog.show();
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