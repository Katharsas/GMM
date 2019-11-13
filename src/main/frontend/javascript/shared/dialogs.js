import $ from "../lib/jquery";
import HtmlPreProcessor from "./preprocessor";
import Errors from "./Errors";

/**
 * Dialogs
 * --------------------------------------------------------------------
 */

var $overlay;
var overlayCount = 0;

const zOverlayDefault = 200;
const zIncrement = 10;
var zOverlayCurrent = zOverlayDefault;

var $confirmDialogContainer;
var $confirmDialogTemplate;


$(document).ready(function() {
	// define stuff
	$confirmDialogContainer = $("#confirmDialog-container");
	$confirmDialogTemplate = $("#confirmDialog-template");
	$overlay = $("#overlay");
	// hide stuff
	$overlay.hide();
	$(".dialog").hide();
	// prep stuff
	const $saveTasksDialog = $("#dialog-saveTasks");
	$saveTasksDialog.find("#dialog-saveTasks-cancelButton").on("click", function() {
		hideDialog($saveTasksDialog);
	});
});

/**
 * Places dialog in center with slight offset to prevent dialogs stacking up in exactly same place.
 * 
 * @param {JQuery} $dialog
 * @param {number} [offsetIndex]  Number of already open dialogs to create offset (default is 0).
 * @param {number} [width]  Width of the dialog (default is min-width from css).
 * @param {number} [height]  Height of the dialog (default is min-width from css).
 */
const centerDialog = function($dialog, offsetIndex, width, height) {
	if (offsetIndex === undefined) offsetIndex = 0;
	if (width === undefined) width = $dialog.outerWidth();
	if (height === undefined) height = $dialog.innerHeight();
	let left = $(window).innerWidth()/2 - width/2;
	let top = ($(window).innerHeight()/2 - height/2) * 0.7;
	left += offsetIndex * 7;
	top += offsetIndex * 7;
	$dialog.css("left", left + "px");
	$dialog.css("top", top + "px");
};

/**
 * @param {JQuery} $dialog
 * @param {number} [width]  Width of the dialog or textarea (default is min-width from css).
 * @param {number} [height]  Height of the dialog or textarea (default is min-width from css).
 */
const setDialogDimensions = function($dialogOrTextarea, width, height) {
	if (width === undefined) width = $dialogOrTextarea.outerWidth();
	$dialogOrTextarea.css("min-width", width+"px");
	if (height === undefined) height = $dialogOrTextarea.innerHeight();
	$dialogOrTextarea.css("min-height", height+"px");
};

const showOverlay = function() {
	$(document.activeElement).blur();
	overlayCount++;
	const zNew = zOverlayDefault + (zIncrement * overlayCount);
	$overlay.css("z-index", zNew);
	$overlay.show();
	zOverlayCurrent = zNew;
};

const hideOverlay = function() {
	overlayCount--;
	if (overlayCount < 0) {
		throw new Errors.IllegalStateException("Overlay was hidden more often than shown!");
	} else if (overlayCount === 0) {
		$overlay.hide();
	}
	const zNew = zOverlayDefault + (zIncrement * overlayCount);
	$overlay.css("z-index", zNew);
};

/**
 * Create a copy of the confirmDialog template and set button callbacks.
 * The dialog must be removed from page manually by the caller.
 */
const createConfirmDialog = function(onConfirm) {
	// copy template
	const { $dialog, actionCancel } = createDialog($confirmDialogTemplate, $confirmDialogContainer);
	// set callbacks
	const $input = $dialog.find(".confirmDialog-input");
	const $textarea = $dialog.find(".confirmDialog-textarea");
	const $ok = $dialog.find(".confirmDialog-ok");
	$ok.on("click", function() {
		onConfirm($input.val(), $textarea.val());
	});
	$dialog.find(".confirmDialog-cancel").on("click", actionCancel);
	return $dialog;
};

/**
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
 * @param {(inputValue:string, textAreaValue:string) => void} onConfirm Callback executes when user hits confirm button (ok).
 * @param {string} message Message to show in the dialog.
 * @param {boolean} hasCancel If true, a cancel button will be shown, which closes the dialog.
 * @param {string} inputDefault If defined, a form input tag will be shown with the argument as its value/text.
 * @param {string} textareaDefault If defined, a form textarea tag will be shown with the argument as its value/text.
 * @param {number} width Min-Width of the dialog or its textarea (defaults to css value).
 * @param {number} height Min-Height of the dialog or its textarea (defaults to css value).
 * @returns {JQuery} The created dialog.
 */
const showConfirmDialog = function(onConfirm, message, hasCancel, inputDefault, textareaDefault, width, height) {
	showOverlay();
	//apply elements and texts to dialog
	const $dialog = createConfirmDialog(onConfirm);
	$dialog.find(".confirmDialog-message").html(message);
	const $input = $dialog.find(".confirmDialog-input");
	const $textarea = $dialog.find(".confirmDialog-textarea");
	const $okButton = $dialog.find(".confirmDialog-ok");
	const $cancelButton = $dialog.find(".confirmDialog-cancel");
	if(inputDefault !== undefined) {
		$input.attr("value", inputDefault);
		$input.show();
	}
	else {
		$input.hide();
	}
	const isTextAreaVisible = textareaDefault !== undefined;
	if(isTextAreaVisible) {
		$textarea.text(textareaDefault);
		$textarea.show();
	}
	else {
		$textarea.hide();
	}
	$cancelButton.toggle(hasCancel);
	//set width and height & center
	setDialogDimensions(isTextAreaVisible ? $textarea : $dialog, width, height);
	const numberOfExisting = $confirmDialogContainer.children().length;
	centerDialog($dialog, numberOfExisting, width, height);
	$dialog.css("z-index", zOverlayCurrent + (zIncrement / 2));
	//show
	$dialog.show();
	if(inputDefault !== undefined) {
		$input.select();
	} else if (isTextAreaVisible) {
		$textarea.focus();
	} else {
		$dialog.focus();
	}
	if (hasCancel) {
		$dialog.on("keyup", function (event) {
			if (event.key === "Escape") {
				$cancelButton.click();
				event.stopPropagation();
			}
		})
	}
	if(!isTextAreaVisible) {
		$dialog.on("keyup", function (event) {
			if (event.key === "Enter") {
				$okButton.click();
				event.stopPropagation();
			}
		});
	}
	return $dialog;
};

/**
 * @param {JQuery} $dialog
 * @param {number} [width]
 * @param {number} [height]
 */
const showDialog = function($dialog, width, height) {
	if ($dialog.hasClass("confirmDialog")) {
		throw new Errors.IllegalArgumentException("Cannot be called on confirm dialog!");
	}
	showOverlay();
	centerDialog($dialog, 0, width, height);
	$dialog.css("z-index", zOverlayCurrent + (zIncrement / 2));
	$dialog.show();
	return $dialog;
};

/**
 * @param {JQuery} $dialog 
 */
const hideDialog = function($dialog) {
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
	
const Dialogs = {
	

	showDialog,
	hideDialog,
	
	/**
	 * Puts an overlay over the page and over any currently opened dialogs. Can be stacked.
	 * Creating more dialogs will position them above this overlay.
	 */
	showOverlay,
	/**
	 * Remove overlay (or one overlay layer if stacked), negating one call of showOverlay() function.
	 */
	hideOverlay,

	/**
	 * Create dialog from arbitrary template. To close dialog call actionCancel.
	 */
	createDialog,
	
	/**
	 * Show a confirmation dialog to the user.
	 * @return {JQuery}
	 * 
	 * @see showConfirmMessage
	 */
	confirm: function(onConfirm, message, textInputDefault, textAreaDefault, width, height) {
		return showConfirmDialog(onConfirm, message, true, textInputDefault, textAreaDefault, width, height);
	},
	
	/**
	 * Show confirmation dialog without cancel button. Closes itself upon confirm, do not hide manually.
	 * // TODO: Make hiding consistent (either always manually or always mostly automatic).
	 * 
	 * @param {(inputValue:string, textAreaValue:string) => void} [onConfirm]
	 * @param {string} message
	 * @param {string} [textInputDefault]
	 * @param {string} [textAreaDefault]
	 * @return {JQuery}
	 * 
	 * @see showConfirmMessage
	 */
	alert: function(onConfirm, message, textInputDefault, textAreaDefault) {
		var $dialog = showConfirmDialog(
			function() {
				hideDialog($dialog);
				if (onConfirm !== null) onConfirm();
			}, message, false, textInputDefault, textAreaDefault);
		return $dialog;
	},
	
	/**
	 * @param {JQueryXHR} jqXHR
	 */
	showException: function(jqXHR) {
		const exception = jqXHR.responseJSON;
		const $exceptionDialog = $("#exceptionDialog");
		$exceptionDialog.find("#exceptionDialog-message").text(exception.message);
		const $instructions = $exceptionDialog.find("#exceptionDialog-instructions");
		const $stackTrace = $exceptionDialog.find("#exceptionDialog-trace");
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


export default Dialogs;
export { centerDialog };