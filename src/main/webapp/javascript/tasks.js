var oldElement;

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
	var activeTab = $(".subTabmenu .tab a[href=\"tasks.htm/reset.htm?tab="+tabName+"&edit="+editName+"\"]").parent();
	activeTab.addClass("activeSubpage");
	
	//make #lists div align with #specificFilters div at bottom
	var $filter = $(".specificFilters");
	var $list = $("#listsMain");
	var height = $filter.offset().top + $filter.outerHeight(false);
	var pos = $list.offset().top + parseInt($list.css("padding-top")) + parseInt($list.css("padding-bottom"));
	$("#listsMain").css("min-height", (height-pos)+"px");
	
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
	switchListElements();
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
	$("#submitTaskButton").click(function() {submitSearchForm();});
	$("#submitSearchButton").click(function() {submitTaskForm();});
	$("#generalFiltersAllCheckbox").change(function() {switchGeneralFiltersAll($(this));});
	$(".generalFiltersFormElement").change(function() {submitGeneralFilters();});
	
});

function newTask() {
	$("#taskForm").show();
	$("#submitTaskButton").show();
	$("#cancelTaskButton").show();
	$("#newTaskButton").hide();
}

function switchListElements() {
	$(".listElementBody").hide();
}

function expandListElements() {
	$(".listElementBody").css("display", "block");
}

function switchListElement(element) {
	var newElement = ($(element).parent())[0];
	
	if(newElement!=oldElement) {
		//hide previous elements footer and border
		$(oldElement).children(".listElementBody").children(".listElementBodyFooter").css("display", "none");
		$(oldElement).css("border-width", "0px");
		//show new elements footer and border
		$(newElement).children(".listElementBody").children(".listElementBodyFooter").css("display", "block");
		$(newElement).css("border-width", "2px");
	}
	
	if(($(element).parent().children(".listElementBody").css("display"))=="none") {
		$(element).parent().children(".listElementBody").css("display", "block");
	}
	else if(newElement==oldElement) {
		$(element).parent().children(".listElementBody").css("display", "none");
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
		$resize.css("width","2em");
		return true;
	}
	$toggle.show();
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