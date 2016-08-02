import $ from "../lib/jquery";
import HtmlPreProcessor from "./preprocessor";
import Errors from "./Errors";

/**
 * Dialogs
 * --------------------------------------------------------------------
 */
var Dialogs = (function() {
	
	var $overlay;
	var $confirmDialogContainer;
	var $confirmDialogTemplate;
	
	var currentCallback;// for non-confirm-dialogs
	
	/**
	 * @param width - int: Width of the dialog (default is min-width from css).
	 * @param height - int: Height of the dialog (default is min-width from css).
	 */
	var centerDialog = function($dialog, width, height) {
		if(width === undefined) width = $dialog.outerWidth();
		if(height === undefined) height = $dialog.innerHeight();
		var left = $(window).innerWidth()/2 - width/2;
		var top = ($(window).innerHeight()/2 - height/2) * 0.7;
		if ($dialog.hasClass("confirmDialog")) {
			var offsetMulti = $confirmDialogContainer.children().length;
			left += offsetMulti * 5;
			top += offsetMulti * 5;
		}
		$dialog.css("left", left + "px");
		$dialog.css("top", top + "px");
	};
	
	var setDialogDimensions = function($dialog, width, height) {
		if(width === undefined)  width = $dialog.outerWidth();
		$dialog.css("min-width", width+"px");
		if(height === undefined) height = $dialog.innerHeight();
		$dialog.css("min-height", height+"px");
	};
	
	var showOverlay = function() {
		$overlay.show();
	};
	var hideOverlay = function() {
		if($confirmDialogContainer.children().length <= 0) {
			$overlay.hide();
		}
	};
	
	/**
	 * Create a copy of the confirmDialog template and set button callbacks.
	 * The dialog must be removed from page manually by the caller.
	 */
	var createConfirmDialog = function(onConfirm) {
		// copy template
		var $dialog = $confirmDialogTemplate.clone();
		$dialog.removeAttr("id");
		HtmlPreProcessor.applyOnlyDataAndEvents($dialog);
		$confirmDialogContainer.append($dialog);
		// set callbacks
		var $input = $dialog.find(".confirmDialog-input");
		var $textarea = $dialog.find(".confirmDialog-textarea");
		var $ok = $dialog.find(".confirmDialog-ok");
		$ok.on("click", function() {
			onConfirm($input.val(), $textarea.val());
		});
		$dialog.find(".confirmDialog-cancel").on("click", function() {
			$dialog.remove();
			hideOverlay();
		});
		return $dialog;
	};
	
	/**
	 * Show a confirmation dialog to the user.
	 * 
	 * @param onConfirm - Function: callback executes when user hits confirm button (ok).
	 * @param message - String: message to show in the dialog.
	 * @param hasCancel - boolean: if true, a cancel button will be shown, which closes the dialog.
	 * @param inputDefault - String: If defined, a form input tag will be shown with the argument as its value/text.
	 * @param textareaDefault - String: If defined, a form textarea tag will be shown with the argument as its value/text.
	 * @param width - int: Width of the dialog (default is min-width from css).
	 * @param height - int: Height of the dialog (default is min-width from css).
	 */
	var showConfirmDialog = function(onConfirm, message, hasCancel, inputDefault, textareaDefault, width, height) {
		showOverlay();
		//apply elements and texts to dialog
		var $dialog = createConfirmDialog(onConfirm);
		$dialog.find(".confirmDialog-message").text(message);
		var $input = $dialog.find(".confirmDialog-input");
		var $textarea = $dialog.find(".confirmDialog-textarea");
		var $cancelButton = $dialog.find(".confirmDialog-cancel");
		if(inputDefault !== undefined) {
			$input.attr("value", inputDefault);
			$input.show();
		}
		else {
			$input.hide();
		}
		if(textareaDefault !== undefined) {
			$textarea.text(textareaDefault);
			$textarea.show();
		}
		else {
			$textarea.hide();
		}
		$cancelButton.toggle(hasCancel);
		//set width and height & center
		setDialogDimensions($dialog, width, height);
		centerDialog($dialog, width, height);
		//show
		$dialog.show();
		if(inputDefault !== undefined) {
			$input.select();
		}
		return $dialog;
	};
	
	
	var showDialog = function($dialog, callback, width, height) {
		if ($dialog.hasClass("confirmDialog")) {
			throw new Errors.IllegalArgumentError("Cannot be called on confirm dialog!");
		}
		currentCallback = callback;
		showOverlay();
		centerDialog($dialog, width, height);
		$dialog.show();
	};
	
	var hideDialog = function($dialog) {
		if ($dialog === undefined) {
			throw new Errors.IllegalArgumentError("Dialog cannot be undefined anymore!");
		}
		if($dialog.hasClass("confirmDialog")) {
			$dialog.remove();
			hideOverlay();
		} else {
			if (!$dialog.hasClass("dialog")) $dialog = $dialog.parents(".dialog");
			$dialog.removeAttr("style");
			$dialog.hide();
			hideOverlay();
		}
	};
	
	var confirmOk = function($dialog) {
		currentCallback();
	};
	
	$(document).ready(function() {
		// define stuff
		$confirmDialogContainer = $("#confirmDialog-container");
		$confirmDialogTemplate = $("#confirmDialog-template");
		$overlay = $("#overlay");
		// hide stuff
		hideOverlay();
		$(".dialog").hide();
		// prep stuff
		var $saveTasksDialog = $("#dialog-saveTasks");
		$saveTasksDialog.find("#dialog-saveTasks-cancelButton").on("click", function() {
			hideDialog($saveTasksDialog);
		});
	});
	
	return {
		
		showDialog: showDialog,
		hideDialog: hideDialog,
		
		showOverlay: showOverlay,
		hideOverlay: hideOverlay,
		
		/**
		 * Show a confirmation dialog to the user.
		 * @see showConfirmMessage
		 */
		confirm: function(onConfirm, message, textInputDefault, textAreaDefault, width, height) {
			return showConfirmDialog(onConfirm, message, true, textInputDefault, textAreaDefault, width, height);
		},
		
		/**
		 * Show confirmation dialog without cancel button.
		 * @see showConfirmMessage
		 */
		alert: function(onConfirm, message, textInputDefault, textAreaDefault) {
			var $dialog = showConfirmDialog(
				function(){
					hideDialog($dialog);
					if (onConfirm !== null) onConfirm();
				}, message, false, textInputDefault, textAreaDefault);
			return $dialog;
		},
		
		showException: function(jqXHR) {
			var $oldDialogs = $(".dialog");
			$oldDialogs.each(function() {
				hideDialog($(this));
			});
			showOverlay();
			
			var exception = jqXHR.responseJSON;
			var $exceptionDialog = $("#exceptionDialog");
			$exceptionDialog.find("#exceptionDialog-message").text(exception.message);
			var $instructions = $exceptionDialog.find("#exceptionDialog-instructions");
			var $stackTrace = $exceptionDialog.find("#exceptionDialog-trace");
			if (exception.stackTrace === null) {
				$instructions.hide();
				$stackTrace.hide();
			} else {
				$instructions.show();
				$stackTrace.text(exception.stackTrace);
				$stackTrace.show();
			}
			showDialog($exceptionDialog);
		},
		
		confirmOk: confirmOk
	};
})();

export default Dialogs;
