var fileName = window.location.pathname.substr(window.location.pathname.lastIndexOf("/")+1);
var paramString = window.location.search.substring(1);


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

function showDialogue($relative, shift, html) {
	var id = $relative.attr('id')+"-dialog";
	$relative.after("<div id='"+id+"' style='display:none;'><div>");
	$dialog = $("#"+id);
	$dialog.addClass("dialogContainer");
	$dialog.html(html);
	
	$dialog.show();
	$("#overlay").show();
}

function hideDialogue() {
	$("#overlay").hide();
}



/**
 * Don't worry about this one.
 */
function testAlert(string) {
	alert('Test Alert! '+string);
}