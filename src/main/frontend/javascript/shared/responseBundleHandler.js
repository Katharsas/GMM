/* jshint esnext:true */
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
 * @param {String} url - URL to the function returning the first messageResponse.
 * 		All subsequent answers (not first) will be sent to the url + "/next".
 * @param {String} responseBundleOption - Key to get option from ResponseBundleOptions,
 * 		either "tasks" or "assets"
 */
export default function(url, responseBundleOption) {
	//Namespace
	var ns = "#batchDialog";
	var ResponseBundleOptions = {
			tasks : {
				conflicts : ["conflict"],
				showButtons : function(conflict, $options) {
					$options.children(ns+"-skipButton").show();
					$options.children(ns+"-doForAllCheckbox").show();
					$options.children(ns+"-overwriteTaskButton").show();
					$options.children(ns+"-addBothTasksButton").show();
				},
				/**
				 * @param {boolean} loadAssets - true if tasks are provided by asset importer,
				 * false if tasks are provided by xml file
				 * @param {string} file - xml task file path, if loadAssets is false
				 */
				start : function(options) {
					var data = options.loadAssets ? {} : {dir: options.file};
					return Ajax.post(url, data);
				}

			},
			assets : {
				conflicts : ["taskConflict", "folderConflict"],
				showButtons : function(conflict, $options) {
					$options.children(ns+"-skipButton").show();
					$options.children(ns+"-doForAllCheckbox").show();
					switch(conflict) {
					case "taskConflict":
						$options.children(ns+"-overwriteTaskAquireDataButton").show();
						$options.children(ns+"-overwriteTaskDeleteDataButton").show();
						break;
					case "folderConflict":
						$options.children(ns+"-aquireDataButton").show();
						$options.children(ns+"-deleteDataButton").show();
						break;
					}
				},
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
	var $checkBox =
			$dialog.find(ns+'-doForAllCheckbox input');
	
	var callback;
	var that = this;
	
	$finishedButton.on("click", function() {
		that.finish();
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
			if(data.message) {
				appendMessage(data.message);
			}
			$finishedButton.show();
		}
		else {
			//if conflict, we need to validate the conflict type
			//and show conflict options accordingly.
			$conflictMessage.html(data.message);
			if (options.conflicts.indexOf(data.status) !== -1) {
				options.showButtons(data.status, $conflictOptions);
			}
			else $conflictMessage.html(outOfSync);
		}
		$messageListContainer.scrollTop($messageList.height());
	}
	
	function appendMessage(message) {
		$messageList.append("<li>"+message+"</li>");
	}
}