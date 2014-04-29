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
			$($oldFile).removeClass(marker);
			$($newFile).addClass(marker);
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
 * Don't worry about this one.
 */
function testAlert(string) {
	alert('Test Alert! '+string);
}