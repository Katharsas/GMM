
function saveTasks() {
	window.location = "admin.htm/saveTasks.htm";
}

function loadTasks() {
	window.location = "admin.htm/loadTasks.htm";
}

$(document).ready( function() {
    $('#fileTreeContainer').fileTree(
    		{
    			root : '/',
    			script : 'jqueryFileTree.jsp'
    		}, function(file) {
        alert(file);
    });
});