
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
	cancelImport();
	//set up jquery fileTree plugin
	$('#fileTreeContainer').fileTree(
		{
			root : '',
			script : 'admin/import'
		},
		function(file) {
			var old = adminVars["selected"];
			$("a[rel='"+old+"']").removeClass("selectedFile");
			$("a[rel='"+file+"']").addClass("selectedFile");
			adminVars["selected"] = file;
		}
	);
});

function addAssetPaths(textures) {
	if(textures) $('#importTexturesButton').show();
	else $('#importMeshesButton').show();
	$('#cancelImportButton').show();
	
	var dir = adminVars["selected"];
	$("#selectedPaths ul").empty();
	$.getJSON("admin/getAssetPaths", { dir: dir, textures: textures }, function(paths) {
		for(var i in paths) {
			$("#selectedPaths ul").append("<li>"+paths[i]+"</li>");
		}
	});
}

function cancelImport() {
	$("#selectedPaths ul").empty();
	$('#importButtons .button').hide();
}

function importTextures() {
	alert("Not yet implemented!");
}
function importMeshes() {
	alert("Not yet implemented!");
}