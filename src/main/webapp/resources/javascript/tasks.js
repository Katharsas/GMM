
var $oldElement = $();

$.expr[':'].blank = function(obj){
	return !$.trim($(obj).text()).length;
};

var tasksVars = {
	"tab":"",
	"edit":"",
	"selectedTaskFileIsAsset":""
};

var tasksFuncs = {
	"tabPar" : function () {
		return "?tab="+(tasksVars.tab === undefined || tasksVars.tab === null ? "" : tasksVars.tab);
	},
	"editPar" : function () {
		return "&edit="+ (tasksVars.edit === undefined || tasksVars.edit === null ? "" :  tasksVars.edit);
	},
	"subDir" : function() {
		return tasksVars.selectedTaskFileIsAsset ? "asset" : "other";
	},
	"filePath" : function() {
		return allVars.selectedTaskFile.attr("rel");
	},
	"refresh" : function () {
		window.location.href = "tasks"+tasksFuncs.tabPar()+"&edit="+tasksVars.edit;
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
	//get subTab and set as active tab / others as inactivetabs
	tasksVars.tab = getURLParameter("tab");
	tasksVars.edit = getURLParameter("edit");
	var $activeTab = $(".subTabmenu .tab a[href=\"tasks/reset?tab="+tasksVars.tab+"&edit="+tasksVars.edit+"\"]").parent();
	$activeTab.addClass("activeSubpage");
	
	//show
	$("#cancelTaskButton").show();
	//hide #taskForm
	if (getURLParameter("edit")==="") {
		$("#taskForm").hide();
		$("#submitTaskButton").hide();
		$("#cancelTaskButton").hide();
		$("#newTaskButton").show();
	}
	else newTask();
	//hide .listElementBody
	collapseListElements();
	//hide comment and details when empty
	$(".elementComments:blank, .elementDetails:blank").hide();
	//hide comment input field & delete question
	$(".commentInput").hide();
	//hide delete question
	$(".elementDelete").hide();
	//set Search according to selected search type (easy or complex)
	setSearchVisibility($("#searchTypeSelect").val());
	//hide search type selector
	$("#searchTypeSelect").hide();
	//hide filter submit
	$("#generalFiltersInvisible").hide();
	//hide generalFilterBody
	if($("#generalFiltersHidden").is(":checked")) toggleGeneralFilters();
	toggleSpecificFilters();//TODO
	
	//listener
	$("#submitTaskButton").click(function() {submitTaskForm();});
	$(".submitSearchButton").click(function() {submitSearchForm();});
	$("#generalFiltersAllCheckbox").change(function() {switchGeneralFiltersAll($(this));});
	$(".generalFiltersFormElement").change(function() {submitGeneralFilters();});
});

function newTask() {
	$("#taskForm").show();
	$("#submitTaskButton").show();
	$("#cancelTaskButton").show();
	$("#newTaskButton").hide();
}

function collapseListElements() {
	$(".listElementBody").hide();
}

function expandListElements() {
	$(".listElementBody").show();
}

function switchListElement(element) {
	var slideTime = 300;
	var $newElement = $(element).parent().first();
	
	if($newElement[0]!=$oldElement[0]) {
		//hide elements that need to be hidden without Focus, hide comment input
		hideCommentInput($oldElement.find(".commentInput"));
		$oldElement.find(".listElementBodyFooter, .elementFiles, .elementPreview").slideUp(slideTime);
//		$oldElement.find(".elementPreview").css("height","50px").show();
//		oldElement.find(".elementPreview").slideDown(slideTime);
		$oldElement.css("border-width", "0px");
		$oldElement.css("padding-left", "8px");
		removeTaskFileTrees($oldElement);
		//show elements that were hidden without focus
		addTaskFileTrees($newElement);
		$newElement.find(".elementPreview").css("height","");
		$newElement.find(".listElementBodyFooter, .elementPreview, .elementFiles").slideDown(slideTime);
		$newElement.css("border-width", "2px");
//		$newElement.css("padding-left", "16px");
		$newElement.css("padding-left", "6px");
	}
	if(($(element).parent().children(".listElementBody").css("display"))=="none") {
		$(element).parent().children(".listElementBody").slideDown(slideTime);
	}
	else if($newElement[0]==$oldElement[0]) {
		$(element).parent().children(".listElementBody").slideUp(slideTime);
		$newElement.css("border-width", "0px");
		$newElement.css("padding-left", "6px");
		$newElement=$();
	}
	$oldElement = $newElement;
}

function addTaskFileTrees($element) {
	var idLink = $element.attr('id');
	var url = idLink+tasksFuncs.tabPar();
	$element.find('#assetFilesContainer').fileTree(
		allFuncs.treePluginOptions("tasks/files/assets/"+url, false),
		function($file) {
			tasksVars.selectedTaskFileIsAsset = true;
			allFuncs.selectTreeElement($file, "selectedTaskFile");
		}
	);
	$element.find('#wipFilesContainer').fileTree(
		allFuncs.treePluginOptions("tasks/files/other/"+url, false),
		function($file) {
			tasksVars.selectedTaskFileIsAsset = false;
			allFuncs.selectTreeElement($file, "selectedTaskFile");
		}
	);
}

function removeTaskFileTrees($element) {
	$element.find('#assetFilesContainer').empty();
	$element.find('#wipFilesContainer').empty();
}

function switchDeleteQuestion(element) {
	var $delete = $(element).parent().parent().children(".elementDelete");
	$delete.toggle();
}

function findSwitchCommentInput(element) {
	switchCommentInput($(element).parents(".listElementBody").find(".commentInput"));
}
function switchCommentInput($commentInput) {
	if($commentInput.is(":visible")) {
		hideCommentInput($commentInput);
	}
	else {
		showCommentInput($commentInput);
	}
}
function hideCommentInput($commentInput) {
	var $elementComments = $commentInput.parent();
	if($elementComments.is(":visible:blank")) {
		$elementComments.hide();
	}
	$commentInput.hide();
}
function showCommentInput($commentInput) {
	$commentInput.parent().show();
	$commentInput.show();
}

/**
 * @param isEasySearch - String or boolean
 */
function setSearchVisibility(isEasySearch) {
//	var slideTime = 300;
	if(isEasySearch.toString()=="true") {
		$(".complexSearch").hide();
		$(".easySearch").show();
//		$(".complexSearch").slideUp(slideTime, function(){$(".easySearch").show();});
	}
	else {
		$(".complexSearch").show();
		$(".easySearch").hide();
//		$(".complexSearch").slideDown(slideTime, function(){$(".easySearch").hide();});
		
	}
}

function switchSearchType() {
	var easySearch = $("#searchTypeSelect").val();
	var newEasySearch = (easySearch!=="true").toString();
	$("#searchTypeSelect").val(newEasySearch);
	setSearchVisibility(newEasySearch);
}

function toggleFilters($toggle, $resize) {
	if($toggle.is(":visible")) {
		$toggle.hide();
//		$toggle.animate({left:'400px'},900);
//		$toggle.hide();
		
		$resize.css("width","2em");
		return true;
	}
	$toggle.show();
//	$toggle.animate({left:'0px'},900);
	$resize.css("width","9em");
	return false;
}

function toggleGeneralFilters() {
	return toggleFilters($("#generalFilterBody"),$(".generalFilters"));
}
function toggleSpecificFilters() {
	return toggleFilters($("#specificFilterBody"),$(".specificFilters"));
}

function switchGeneralFilters() {
	$("#generalFiltersHidden").prop("checked", toggleGeneralFilters());
	submitGeneralFilters();
}

function switchSpecificFilters() {
	//TODO
	toggleSpecificFilters();
}

function switchGeneralFiltersAll($element){
	$(".generalFiltersAllCheckBoxTarget").attr("checked", $element.is(":checked"));
	submitGeneralFilters();
}

function submitSearchForm(){
	$("#searchForm").submit();
}

function submitTaskForm(){
	$("#taskForm").submit();
}

function submitGeneralFilters(){
	$(".generalFilters").submit();
}

function uploadFile(input, idLink) {
	var file = input.files[0];
	var uri = "tasks/upload/"+idLink+tasksFuncs.tabPar();
	
	sendFile(file, uri, function(responseText) {
		tasksFuncs.refresh();
//		alert("Server Response: "+responseText);
	});
}

function downloadFile(idLink) {
	var dir = tasksFuncs.filePath();
	if(dir === undefined || dir === "") {return;}
	var uri = "tasks/download/" + idLink + "/" + tasksFuncs.subDir() + "/" + dir + "/" + tasksFuncs.tabPar();
	window.open(uri);
}

function confirmDeleteFile(idLink) {
	var dir = tasksFuncs.filePath();
	if(dir === undefined || dir === "") {return;}
	confirm(function() {
				var assetPar = "&asset="+tasksVars.selectedTaskFileIsAsset.toString();
				$.post("tasks/deleteFile/"+idLink+tasksFuncs.tabPar()+assetPar, { dir: dir }, function() {
					tasksFuncs.refresh();
				});
			},
			"Delete "+tasksFuncs.filePath()+" ?");
}

function confirmDeleteTask(idLink, name) {
	confirm(function() {
				window.location = "tasks/deleteTask/"+idLink+tasksFuncs.tabPar();
			},
			"Delete task \'"+name+"\' ?");
}