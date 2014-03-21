var oldElement;

$.expr[':'].blank = function(obj){
	return !$.trim($(obj).text()).length;
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
	var tabName = getURLParameter("tab");
	var editName = getURLParameter("edit");
	var activeTab = $(".subTabmenu .tab a[href=\"tasks/reset?tab="+tabName+"&edit="+editName+"\"]").parent();
	activeTab.addClass("activeSubpage");
	
	//show
	$("#cancelTaskButton").show();
	//hide #taskForm
	if (getURLParameter("edit")=="") {
		$("#taskForm").hide();
		$("#submitTaskButton").hide();
		$("#cancelTaskButton").hide();
		$("#newTaskButton").show();
	}
	else newTask();
	//hide .listElementBody
	collapseListElements();
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
	var newElement = ($(element).parent())[0];
	
	if(newElement!=oldElement) {
		//hide previous elements footer and border
		$(oldElement).find(".listElementBody > *:blank").hide();
		$(oldElement).find(".listElementBody > .listElementBodyFooter").slideUp(slideTime);
		$(oldElement).css("border-width", "0px");
		//show new elements footer and border
		$(newElement).find(".listElementBody > .listElementBodyFooter").slideDown(slideTime);
		$(newElement).css("border-width", "1px");
	}
	
	if(($(element).parent().children(".listElementBody").css("display"))=="none") {
		$(element).parent().children(".listElementBody").slideDown(slideTime);
	}
	else if(newElement==oldElement) {
		$(element).parent().children(".listElementBody").slideUp(slideTime);
		$(newElement).css("border-width", "0px");
		newElement=null;
	}
	oldElement = newElement;
}

function switchDeleteQuestion(element) {
	var $delete = $(element).parent().parent().children(".elementDelete");
	$delete.toggle();
}

function switchCommentInput(element) {
	var $commentInput = $(element).parent().parent().children(".elementComments").children(".commentInput");
	$commentInput.toggle();
}

/**
 * @param isEasySearch - String or boolean
 */
function setSearchVisibility(isEasySearch) {
	if(isEasySearch.toString()=="true") {
		$(".complexSearch").hide();
		$(".easySearch").show();
	}
	else {
		$(".complexSearch").show();
		$(".easySearch").hide();
	}
}

function switchSearchType() {
	var easySearch = $("#searchTypeSelect").val();
	var newEasySearch = (!(easySearch=="true")).toString();
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