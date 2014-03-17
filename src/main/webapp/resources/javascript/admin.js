
function saveTasks() {
	window.location = "admin/saveTasks";
}

function loadTasks() {
	window.location = "admin/loadTasks";
}

function deleteTasks() {
	window.location = "admin/deleteTasks";
}

function importAssets() {
	alert("Not yet implemented");
}

var adminVars = {
	"selected":""
};

$(document).ready( function() {
    $('#fileTreeContainer').fileTree(
    		{
    			root : '/',
    			script : 'jqueryFileTree.jsp'
    		}, function(file) {
        alert(file);
    });
	
	 $('#fileTreeContainer').fileTree(
			{
				root : '',
				script : 'admin/import'
			}, function(file) {
		var old = adminVars["selected"];
		$("a[rel='"+old+"']").removeClass("selectedFile");
		$("a[rel='"+file+"']").addClass("selectedFile");
		adminVars["selected"] = file;
	});
});

function addFile() {
	var file = adminVars["selected"];
	$("#selectedPaths ul").append("<li>"+file+"</li>");
}