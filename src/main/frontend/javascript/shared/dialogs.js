import $ from "../lib/jquery";
import HtmlPreProcessor from "./preprocessor";
import Errors from "./Errors";

/**
 * Dialogs
 * --------------------------------------------------------------------
 */

/**
 * Places dialog in center with slight offset to prevent dialogs stacking up in exactly same place.
 * @param {number} offsetIndex - int (optional): Number of already open dialogs to create offset (defaul is 0).
 * @param {number} width - int (optional): Width of the dialog (default is min-width from css).
 * @param {number} height - int (optional): Height of the dialog (default is min-width from css).
 */
const centerDialog = function($dialog, offsetIndex, width, height) {
	if (offsetIndex === undefined) offsetIndex = 0;
	if(width === undefined) width = $dialog.outerWidth();
	if(height === undefined) height = $dialog.innerHeight();
	var left = $(window).innerWidth()/2 - width/2;
	var top = ($(window).innerHeight()/2 - height/2) * 0.7;
	if ($dialog.hasClass("confirmDialog")) {
		left += offsetIndex * 5;
		top += offsetIndex * 5;
	}
	$dialog.css("left", left + "px");
	$dialog.css("top", top + "px");
};

var Dialogs = (function() {
	
	var $overlay;
	var $confirmDialogContainer;
	var $confirmDialogTemplate;

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
		const { $dialog, actionCancel } = createDialog($confirmDialogTemplate, $confirmDialogContainer);
		// set callbacks
		var $input = $dialog.find(".confirmDialog-input");
		var $textarea = $dialog.find(".confirmDialog-textarea");
		var $ok = $dialog.find(".confirmDialog-ok");
		$ok.on("click", function() {
			onConfirm($input.val(), $textarea.val());
		});
		$dialog.find(".confirmDialog-cancel").on("click", actionCancel);
		return $dialog;
	};

	/**
	 * Create dialog from arbitrary template. To close call actionCancel.
	 * @param {JQuery} $template 
	 * @param {JQuery} $container 
	 */
	const createDialog = function($template, $container) {
		// copy template
		const $dialog = $template.clone();
		$dialog.removeAttr("id");
		HtmlPreProcessor.applyOnlyDataAndEvents($dialog);
		$container.append($dialog);
		// 	const $ok = $dialog.find(".dialog-ok");
		const actionCancel = function() {
			$dialog.remove();
			hideOverlay();
		}
		return { 
			$dialog,
			actionCancel
		};
	}
	
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
		$dialog.find(".confirmDialog-message").html(message);
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
		var numberOfExisting = $confirmDialogContainer.children().length;
		centerDialog($dialog, numberOfExisting, width, height);
		//show
		$dialog.show();
		if(inputDefault !== undefined) {
			$input.select();
		}
		return $dialog;
	};
	
	/**
	 * @param {JQuery} $dialog
	 * @param {number} width - optional
	 * @param {number} height - optional
	 */
	var showDialog = function($dialog, width, height) {
		if ($dialog.hasClass("confirmDialog")) {
			throw new Errors.IllegalArgumentException("Cannot be called on confirm dialog!");
		}
		showOverlay();
		centerDialog($dialog, 0, width, height);
		$dialog.show();
		return $dialog;
	};
	
	/**
	 * @param {JQuery} $dialog 
	 */
	var hideDialog = function($dialog) {
		if ($dialog === undefined) {
			throw new Errors.IllegalArgumentException("Dialog cannot be undefined!");
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
	
	return {
		
		showDialog: showDialog,
		hideDialog: hideDialog,
		
		showOverlay: showOverlay,
		hideOverlay: hideOverlay,

		createDialog: createDialog,
		
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
		}
	};
})();

export default Dialogs;
export { centerDialog };