import $ from "../lib/jquery";
import { allVars } from "./default";

/**
 * Dialogs
 * --------------------------------------------------------------------
 */
var Dialogs = (function() {
	
	/**
	 * @param width - int: Width of the dialog (default is min-width from css).
	 * @param height - int: Height of the dialog (default is min-width from css).
	 */
	var centerDialog = function($dialog, width, height) {
		if(width === undefined)  width = $dialog.outerWidth();
		$dialog.css("left", ($(window).innerWidth()/2-width/2)+"px");
		if(height === undefined) height = $dialog.innerHeight();
		$dialog.css("top", (($(window).innerHeight()/2-height/2)*0.7)+"px");
	};
	
	var setDialogDimensions = function($dialog, width, height) {
		if(width === undefined)  width = $dialog.outerWidth();
		$dialog.css("min-width", width+"px");
		if(height === undefined) height = $dialog.innerHeight();
		$dialog.css("min-height", height+"px");
	};
	
	var showOverlay = function() {
		allVars.$overlay.show();
	};
	var hideOverlay = function() {
		allVars.$overlay.hide();
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
		var $confirmDialog = $("#confirmDialog");
		$confirmDialog.find("#confirmDialog-message").text(message);
		var $input = $confirmDialog.find("#confirmDialog-input");
		var $textarea = $confirmDialog.find("#confirmDialog-textarea");
		allVars.onConfirmCallback = function() {
			if(onConfirm !== null) onConfirm($input.val(), $textarea.val());
		};
		var $cancelButton = $confirmDialog.find("#confirmDialog-cancel");
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
		if(hasCancel) {
			$cancelButton.show();
		}
		else {
			$cancelButton.hide();
		}
		//set width and height & center
		setDialogDimensions($confirmDialog, width, height);
		centerDialog($confirmDialog, width, height);
		//show
		$confirmDialog.show();
		if(inputDefault !== undefined) {
			$input.select();
		}
		return $confirmDialog;
	};
	
	
	var showDialog = function($dialog, width, height) {
		showOverlay();
		centerDialog($dialog, width, height);
		$dialog.show();
	};
	
	var hideDialog = function($dialog) {
		if ($dialog === undefined) $dialog = $(".dialog");
		if (!$dialog.hasClass("dialog")) $dialog = $dialog.parents(".dialog");
		$dialog.removeAttr("style");
		$dialog.hide();
		hideOverlay();
	};
	
	var confirmOk = function() {
		allVars.onConfirmCallback();
	};
	
	$(document).ready(function() {
		var $confirmDialog = $("#confirmDialog");
		$confirmDialog.find("#confirmDialog-ok").on("click", confirmOk);
		$confirmDialog.find("#confirmDialog-cancel").on("click", function() {
			hideDialog($confirmDialog);
		});
		var $saveTasksDialog = $("#dialog-saveTasks");
		$saveTasksDialog.find("#dialog-saveTasks-cancelButton").on("click", function() {
			hideDialog($saveTasksDialog);
		});
	});
	
	return {
		showDialog: showDialog,
		hideDialog: hideDialog,
		
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
			hideDialog();
			showOverlay();
			
			var exception = $.parseJSON(jqXHR.responseText);
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
