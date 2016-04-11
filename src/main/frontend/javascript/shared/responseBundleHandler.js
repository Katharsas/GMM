import $ from "../lib/jquery";
import Ajax from "./ajax";
import Dialogs from "./dialogs";

/**
 * -------------------- ResponseBundleHandler ----------------------------------
 * Provides a way to communicate with the server when the server needs to send
 * a lot of messages to the client and the client needs to be able to respond
 * to any of those messages.
 * 
 * To not waste requests, the messages from the server will come in bundles of
 * dynamic size, where the last bundle message either indicates that the server
 * needs a message from the client or that the server has finished sending all
 * messages or the server wants to give an update because he is very slow.
 * 
 * @author Jan Mothes
 * 
 * @param {string} url - URL to the function returning the first messageResponse.
 * 		All subsequent answers (not first) will be sent to the url + "/next".
 * @param {string} responseBundleOption - Key to get option from ResponseBundleOptions,
 * 		either "tasks" or "assets"
 * @param {boolean} minimalUI - currently only hides doForAll checkbox (use this when
 * 		only one / low number of elements get processed)
 */
export default function ResponseBundleHandler(url, responseBundleOption, minimalUI) {
	minimalUI = minimalUI || false;
	//Namespace
	var ns = "#batchDialog";
	var ResponseBundleOptions = {
			tasks : {
				conflicts : [{
					name: "conflict",
					actions: ["skip", "overwrite", "both"]
				}],
				/**
				 * @param {string} file - path to xml task file
				 */
				start : function(options) {
					return Ajax.post(url, {dir: options.file});
				}

			},
			assets : {
				conflicts : [{
					name: "taskConflict",
					actions: ["skip", "overwriteTaskAquireData", "overwriteTaskDeleteData"]
				},{
					name: "folderConflict",
					actions: ["skip", "aquireData", "deleteData"]
				}],
				/**
				 * @param {jquery} $taskForm - task form that will be submitted
				 */
				start : function(options) {
					return Ajax.post(url, {}, options.$taskForm);
				}
			}
		};
	var options = ResponseBundleOptions[responseBundleOption];
	
	var $dialog = $(ns);
	var $messageListContainer =
			$dialog.find(ns+"-listWrapper");
	var $messageList =
			$dialog.find(ns+"-list");
	var $conflictMessage  =
			$dialog.find(ns+"-conflictMessage");
	var $conflictOptions =
			$dialog.find(ns+"-conflictOptions");
	var $finishedButton =
			$dialog.find(ns+"-finishLoadingButton");
	var $checkBoxContainer =
			$dialog.find(ns+'-doForAllCheckbox');
	var $checkBox = $checkBoxContainer.find("input");
	var $conflictActionButtons = $conflictOptions.find(".button");
	
	var callback;
	var that = this;
	
	$finishedButton.off("click").on("click", function() {
		that.finish();
	});
	
	$conflictActionButtons.off("click").on("click", function() {
		that.answer($(this).attr("data-action"));
	});
	
	/**
	 * Get first responses (bundled) from server. Last response in bundle is either either "finish"
	 * or conflict message.
	 * @param {any} startOptions - see start method from chosen ResponseBundleOption
	 * @param {function} onFinished - callback
	 */
	this.start = function (startOptions, onFinished) {
		callback = onFinished;
		$conflictOptions.children().hide();
		$finishedButton.hide();
		var ajaxResult = options.start(startOptions);
		if (ajaxResult === undefined) return;
		ajaxResult.done(reactToResults);
		Dialogs.showDialog($dialog);
	};
	
	/**
	 * If a conflict occured at last bundle, use this method to give an answer for conflict handling.
	 * Server will return another response bundle.
	 */
	this.answer = function (answer) {
		$conflictOptions.children().hide();
		$conflictMessage.empty();
		var doForAll = $checkBox.is(":checked");
		Ajax.post(url + "/next", { operation: answer, doForAll: doForAll })
			.done(reactToResults);
	};
	
	this.finish = function () {
		Dialogs.hideDialog($dialog);
		$messageList.empty();
		if (callback !== undefined) callback();
	};
	
	/**
	 * process server response bundle
	 */
	function reactToResults(results) {
		var outOfSync = "Something went wrong! Out of sync with server. Please reload page.";
		var multiConflicts = "Error: Multiple actions with same name defined. Please contact admin.";
		//for all but the last element, the status should be success
		for (var i = 0; i < results.length-1; i++) {
			var dataToCheck = results[i];
			if(dataToCheck.status == "success") {
				appendMessage(dataToCheck.message);
			}
			else $conflictMessage.html(outOfSync);
		}
		//last result can be anything
		var data = results[results.length-1];
		if(data.status == "success") {
			//if success, the server can go on with next package
			appendMessage(data.message);
			that.answer("default");
		}
		else if(data.status == "finished") {
			// server finished, show finished message & button
			if(data.message) {
				appendMessage(data.message);
			}
			$finishedButton.show();
		}
		else {
			//if conflict, we need to validate the conflict type
			//and show conflict options accordingly.
			$conflictMessage.html(data.message);
			var foundConflict = false;
			options.conflicts.forEach(function(conflict){
				if(conflict.name === data.status) {
					if (foundConflict) {
						$conflictMessage.html(multiConflicts);
					}
					foundConflict = true;
					showActionButtons(conflict.actions);
				}
			});
			if (!foundConflict) {
				$conflictMessage.html(outOfSync);
			}
		}
		$messageListContainer.scrollTop($messageList.height());
	}
	
	function appendMessage(message) {
		$messageList.append("<li>"+message+"</li>");
	}
	
	function showActionButtons(actions) {
		if (!minimalUI) {
			$checkBoxContainer.show();
		}
		actions.forEach(function(action) {
			$conflictActionButtons.filter("[data-action='"+action+"']").show();
		});
	}
}