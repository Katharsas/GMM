
function saveTasks() {
	window.location = "admin/saveTasks";
}

function loadTasks() {
	window.location = "admin/loadTasks";
}

function deleteTasks() {
	window.location = "admin/deleteTasks";
}

$(document).ready( function() {
//    $('#fileTreeContainer').fileTree(
//    		{
//    			root : '/',
//    			script : 'jqueryFileTree.jsp'
//    		}, function(file) {
//        alert(file);
//    });
	
	 $('#fileTreeContainer').fileTree(
	    		{
	    			root : '',
	    			script : 'admin/import'
	    		}, function(file) {
	        alert(file);
	    });
});