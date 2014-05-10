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

function confirm(onConfirm, message) {
	allVars.onConfirmCallback = onConfirm;
	$confirmDialog = $("#confirmDialog");
	$confirmDialog.find("#confirmDialogMessage").text(message);
	$("#overlay").show();
	$confirmDialog.show();
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